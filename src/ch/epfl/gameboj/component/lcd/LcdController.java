package ch.epfl.gameboj.component.lcd;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

public final class LcdController implements Component, Clocked {

    public static final int LCD_WIDTH = 160;
    public static final int LCD_HEIGHT = 144;

    private static final int BACKGROUND_WIDTH = 256;
    private static final int TOTAL_AMOUNT_OF_LINE = 153;

    private static final int CYCLES_SPENT_IN_MODE_0 = 51;
    private static final int CYCLES_SPENT_IN_MODE_2 = 20;
    private static final int CYCLES_SPENT_IN_MODE_3 = 43;
    private static final int CYCLES_NEEDED_TO_DRAW_LINE = CYCLES_SPENT_IN_MODE_0
            + CYCLES_SPENT_IN_MODE_2 + CYCLES_SPENT_IN_MODE_3;

    private static final int CYCLES_SPENT_BEFORE_MODE_0 = CYCLES_SPENT_IN_MODE_2
            + CYCLES_SPENT_IN_MODE_3;
    private static final int CYCLES_SPENT_BEFORE_MODE_1 = LCD_HEIGHT
            * CYCLES_NEEDED_TO_DRAW_LINE;
    private static final int CYCLES_SPENT_BEFORE_MODE_2 = 0;
    private static final int CYCLES_SPENT_BEFORE_MODE_3 = CYCLES_SPENT_IN_MODE_2;

    private static final int MASK_REMOVE_ONE_LSB = 0b1111_1110;
    private static final int MASK_REMOVE_TWO_LSB = 0b1111_1100;
    private static final int MASK_REMOVE_THREE_LSB = 0b1111_1000;

    private static final int TILE_SIZE = 8;
    private static final int TILES_PER_LINE = 32;

    private static final int LAST_TILE_SOURCE_RANGE = 0x9000;

    private enum LineToSet {
        LY, LYC;
    }

    private enum Reg implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    private enum LCDCBits implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }

    private enum STATBits implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED
    }

    private static final STATBits[] interruptsSetting = { STATBits.INT_MODE0,
            STATBits.INT_MODE1, STATBits.INT_MODE2 };

    private enum SpritePropertiesBits implements Bit {
        UNUSED0, UNUSED1, UNUSED2, UNUSED3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }

    private static final Reg[] REGISTER_TABLE = Reg.values();

    private RegisterFile<Register> regF = new RegisterFile<>(REGISTER_TABLE);
    private RamController videoRamController = new RamController(
            new Ram(AddressMap.VIDEO_RAM_SIZE), AddressMap.VIDEO_RAM_START);
    private RamController oamController = new RamController(
            new Ram(AddressMap.OAM_RAM_SIZE), AddressMap.OAM_START);
    private Cpu cpu;
    private Bus bus;
    private LcdImage.Builder nextImageBuilder;
    private LcdImage currentImage;

    private long nextNonIdleCycle = Long.MAX_VALUE;
    private long startCycleOfImage = Long.MAX_VALUE;
    private int windowLineIndex;
    private boolean isInDMAMode;

    private int bytesWritten;
    // Stored outside the dma function to be calculated once instead of 160
    // times
    private int dmaAddress;

    /**
     * Creates a new LcdController able to communicate with the given cpu
     * 
     * @param cpu
     *            The cpu that will be linked with the new LcdController (must
     *            not be null)
     * @throws NullPointerException
     *             if the given cpu is null
     */
    public LcdController(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
        // Creates an empty currentImage in case currentImage() is called before
        // a real image has been built
        currentImage = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT).build();
    }

    /**
     * Returns the current LcdImage stored by the LcdController
     * 
     * @return the current LcdImage stored by the LcdController
     */
    public LcdImage currentImage() {
        return currentImage;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (isInRegistersBounds(address)) {
            int translatedAddress = address - AddressMap.REGS_LCDC_START;
            return regF.get(REGISTER_TABLE[translatedAddress]);
        } else if (isInOAMBounds(address)) {
            return oamController.read(address);
        } else if (isInVideoRamBounds(address)) {
            return videoRamController.read(address);
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int value) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(value);
        if (isInRegistersBounds(address)) {
            int translatedAddress = address - AddressMap.REGS_LCDC_START;
            Reg register = REGISTER_TABLE[translatedAddress];
            switch (register) {
            case LY:
                // Intentionnaly empty
                break;
            case LYC:
                setLine(LineToSet.LYC, value);
                break;
            case STAT: {
                int unaffectedValue = Bits.clip(3, regF.get(Reg.STAT));
                // Faster than extract and shift
                int newValue = (value & MASK_REMOVE_THREE_LSB)
                        | unaffectedValue;
                regF.set(register, newValue);
                break;
            }
            case LCDC:
                if (regF.testBit(register, LCDCBits.LCD_STATUS)
                        && !Bits.test(value, LCDCBits.LCD_STATUS)) {
                    deactivateLCDController();
                }
                regF.set(register, value);
                break;
            case DMA:
                isInDMAMode = true;
                bytesWritten = 0;
                dmaAddress = value << 8;
                regF.set(register, value);
                break;
            default:
                regF.set(register, value);
            }
        } else if (isInOAMBounds(address)) {
            oamController.write(address, value);
        } else if (isInVideoRamBounds(address)) {
            videoRamController.write(address, value);
        }
    }

    @Override
    public void attachTo(Bus bus) {
        bus.attach(this);
        this.bus = bus;
    }

    @Override
    public void cycle(long cycle) {
        if (!regF.testBit(Reg.LCDC, LCDCBits.LCD_STATUS))
            return;
        if (nextNonIdleCycle == Long.MAX_VALUE) {
            nextNonIdleCycle = cycle;
        }
        if (isInDMAMode)
            transferByte();
        if (cycle >= nextNonIdleCycle)
            updateMode(cycle);

    }

    private void transferByte() {
        int byteTransfered = bus.read(dmaAddress + bytesWritten);
        oamController.write(AddressMap.OAM_START + bytesWritten,
                byteTransfered);
        bytesWritten++;
        if (bytesWritten == AddressMap.OAM_RAM_SIZE - 1) {
            isInDMAMode = false;
        }
    }

    private void updateMode(long cycle) {

        if (startCycleOfImage == Long.MAX_VALUE)
            startCycleOfImage = cycle;

        int globalInterval = (int) (cycle - startCycleOfImage);
        int lineInterval = globalInterval % CYCLES_NEEDED_TO_DRAW_LINE;

        if (globalInterval >= CYCLES_SPENT_BEFORE_MODE_1) {
            // Handle possible switch to mode 1
            if (globalInterval == CYCLES_SPENT_BEFORE_MODE_1) {
                setMode(1);
                currentImage = nextImageBuilder.build();
            }
            int currentLine = globalInterval / CYCLES_NEEDED_TO_DRAW_LINE;
            setLine(LineToSet.LY, currentLine);
            nextNonIdleCycle += CYCLES_NEEDED_TO_DRAW_LINE;
            // Signal that it is time to restart the whole process
            if (currentLine == TOTAL_AMOUNT_OF_LINE)
                startCycleOfImage = Long.MAX_VALUE;
        } else {
            switch (lineInterval) {
            // Handle mode 0
            case CYCLES_SPENT_BEFORE_MODE_0:
                setMode(0);
                nextNonIdleCycle += CYCLES_SPENT_IN_MODE_0;
                break;

            // Handle mode 2
            case CYCLES_SPENT_BEFORE_MODE_2:
                // Creates a new LcdImageBuilder if it the first time that this
                // mode has been entered since the creation of a new frame
                if (globalInterval == 0) {
                    nextImageBuilder = new LcdImage.Builder(LCD_WIDTH,
                            LCD_HEIGHT);
                    windowLineIndex = 0;
                }

                setLine(LineToSet.LY,
                        globalInterval / CYCLES_NEEDED_TO_DRAW_LINE);
                setMode(2);
                nextNonIdleCycle += CYCLES_SPENT_IN_MODE_2;
                break;

            // Handle mode 3
            case CYCLES_SPENT_BEFORE_MODE_3:
                setMode(3);
                computeLine();
                nextNonIdleCycle += CYCLES_SPENT_IN_MODE_3;
                break;
            default:
                // The LcdController is in between two modes which is an
                // unexpected behaviour and should be signalled by an error
                throw new IllegalStateException(
                        "LcdController in unexpected state");
            }
        }

    }

    private void setMode(int mode) {
        Preconditions.checkArgument(0 <= mode && mode < 4);
        regF.set(Reg.STAT, (regF.get(Reg.STAT) & MASK_REMOVE_TWO_LSB) | mode);
        if (mode == 1)
            cpu.requestInterrupt(Interrupt.VBLANK);
        if (mode != 3 && regF.testBit(Reg.STAT, interruptsSetting[mode]))
            cpu.requestInterrupt(Interrupt.LCD_STAT);

    }

    // Main method to calculate a line of the Image
    private void computeLine() {

        int currentLine = regF.get(Reg.LY);
        boolean tileSource = regF.testBit(Reg.LCDC, LCDCBits.TILE_SOURCE);

        LcdImageLine line = LcdImageLine.EMPTY_LCD_WIDTH_LINE;

        // Creates the background line
        if (regF.testBit(Reg.LCDC, LCDCBits.BG)) {
            int xOffset = regF.get(Reg.SCX);
            int lineIndex = Bits.clip(8, currentLine + (regF.get(Reg.SCY)));
            LcdImageLine bgLine = computeLineOf(LCDCBits.BG_AREA, lineIndex,
                    BACKGROUND_WIDTH, tileSource);
            line = bgLine.extractWrapped(xOffset, LCD_WIDTH);
        }

        // Must be declared out of the if statement because potentially used
        // later
        LcdImageLine spriteLineBeforeBG = LcdImageLine.EMPTY_LCD_WIDTH_LINE;

        // Note : This way of calculating both sprite lines is the fastest
        // because it doesn't have to iterate twice over the sprites thus it was
        // chosen even though it requires that the foreground sprites are
        // declared outside of the if statement
        boolean spritesEnabled = regF.testBit(Reg.LCDC, LCDCBits.OBJ);
        if (spritesEnabled) {
            int spriteHeight = regF.testBit(Reg.LCDC, LCDCBits.OBJ_SIZE) ? 16
                    : 8;
            LcdImageLine spriteLineBehindBG = LcdImageLine.EMPTY_LCD_WIDTH_LINE;

            int[] sprites = spritesIntersectingLine(currentLine, spriteHeight);
            for (int i = 0; i < sprites.length; ++i) {
                int spriteAddress = AddressMap.OAM_START + sprites[i] * 4;

                int spriteProperties = oamController.read(spriteAddress + 3);
                LcdImageLine spriteLine = getLineCorrespondingToSprite(
                        spriteAddress, currentLine, spriteHeight,
                        spriteProperties);

                if (Bits.test(spriteProperties, SpritePropertiesBits.BEHIND_BG))
                    spriteLineBehindBG = spriteLine.below(spriteLineBehindBG);
                else
                    spriteLineBeforeBG = spriteLine.below(spriteLineBeforeBG);

            }
            BitVector opacity = calculateOpacity(line, spriteLineBehindBG);
            line = spriteLineBehindBG.below(line, opacity);
            // We must wait until the window has been added before we add the
            // spriteLineBeforeBG
        }

        // Creates the window line
        int adjustedWX = Math.max(0, regF.get(Reg.WX) - 7);
        boolean winEnabled = regF.testBit(Reg.LCDC, LCDCBits.WIN)
                && adjustedWX < LCD_WIDTH && regF.get(Reg.WY) <= currentLine;

        if (winEnabled) {
            LcdImageLine windowLine = computeLineOf(LCDCBits.WIN_AREA,
                    windowLineIndex, LCD_WIDTH, tileSource);
            windowLineIndex++;
            line = line.join(windowLine, adjustedWX);
        }

        if (spritesEnabled)
            line = line.below(spriteLineBeforeBG);

        nextImageBuilder.setLine(line, currentLine);
    }

    private int[] spritesIntersectingLine(int line, int spriteHeight) {
        int[] sprites = new int[10];
        int next = 0;
        for (int i = AddressMap.OAM_START; i < AddressMap.OAM_END
                && next < sprites.length; i += 4) {
            int yTopCoord = oamController.read(i) - 16;
            if (yTopCoord <= line && line < yTopCoord + spriteHeight)
                sprites[next++] = Bits.make16(oamController.read(i + 1),
                        (i - AddressMap.OAM_START) / 4);
        }
        Arrays.sort(sprites, 0, next);
        int[] onlySpriteIndexes = new int[next];
        for (int i = 0; i < next; ++i) {
            onlySpriteIndexes[i] = Bits.clip(8, sprites[i]);
        }
        return onlySpriteIndexes;
    }

    private LcdImageLine getLineCorrespondingToSprite(int spriteAddress,
            int lineIndex, int spriteSize, int spriteProperties) {
        LcdImageLine.Builder builder = new LcdImageLine.Builder(LCD_WIDTH);
        int yTopCoord = oamController.read(spriteAddress) - 16;
        int xLeftCoord = oamController.read(spriteAddress + 1) - 8;
        int lineOfTile = (lineIndex - yTopCoord) * 2;
        boolean isFlippedHorizontally = Bits.test(spriteProperties,
                SpritePropertiesBits.FLIP_H);

        if (Bits.test(spriteProperties, SpritePropertiesBits.FLIP_V))
            lineOfTile = spriteSize * 2 - lineOfTile - 2;

        int tileAddress = oamController.read(spriteAddress + 2);

        if (spriteSize == 16)
            tileAddress &= MASK_REMOVE_ONE_LSB;

        int lowByte = extractLineOfTile(tileAddress, lineOfTile, true,
                !isFlippedHorizontally);
        int highByte = extractLineOfTile(tileAddress, lineOfTile + 1, true,
                !isFlippedHorizontally);

        LcdImageLine line = builder.setBytes(highByte, lowByte, 0).build();
        line = Bits.test(spriteProperties, SpritePropertiesBits.PALETTE)
                ? line.mapColors(regF.get(Reg.OBP1))
                : line.mapColors(regF.get(Reg.OBP0));

        return line.shift(xLeftCoord);
    }

    private BitVector calculateOpacity(LcdImageLine bgLine,
            LcdImageLine spriteLine) {
        BitVector bgOpacity = bgLine.getOpacity();
        BitVector spriteOpacity = spriteLine.getOpacity();
        // equivalent to T = ¬(¬BO & SO)
        return ((bgOpacity.not()).and(spriteOpacity)).not();
    }

    // Calculate the line of a certain region for example the background line
    // or window line
    private LcdImageLine computeLineOf(LCDCBits bitToTest, int lineIndex,
            int widthOfLine, boolean tileSource) {

        int tileIndexAddress = regF.testBit(Reg.LCDC, bitToTest)
                ? AddressMap.BG_DISPLAY_DATA[1]
                : AddressMap.BG_DISPLAY_DATA[0];

        int yOffset = (lineIndex / TILE_SIZE) * TILES_PER_LINE;
        int lineOfTile = (lineIndex % TILE_SIZE) * 2;
        int startOfLine = tileIndexAddress + yOffset;

        LcdImageLine.Builder builder = new LcdImageLine.Builder(widthOfLine);
        for (int i = 0; i < widthOfLine / TILE_SIZE; ++i) {
            int address = videoRamController.read(startOfLine + i);
            int lowByte = extractLineOfTile(address, lineOfTile, tileSource,
                    true);
            int highByte = extractLineOfTile(address, lineOfTile + 1,
                    tileSource, true);
            builder.setBytes(highByte, lowByte, i);
        }
        return builder.build().mapColors(regF.get(Reg.BGP));
    }

    private int extractLineOfTile(int address, int lineOfTile,
            boolean tileSource, boolean shouldReverse) {
        int tileRangeStart = (tileSource || address > 0x7F)
                ? AddressMap.TILE_SOURCE[1]
                : LAST_TILE_SOURCE_RANGE;
        int data = bus.read(tileRangeStart + address * 16 + lineOfTile);
        return shouldReverse ? Bits.reverse8(data) : data;

    }

    private void deactivateLCDController() {
        setMode(0);
        setLine(LineToSet.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
        startCycleOfImage = Long.MAX_VALUE;

    }

    private void setLine(LineToSet option, int value) {
        regF.set(option == LineToSet.LY ? Reg.LY : Reg.LYC, value);

        if (regF.get(Reg.LY) == regF.get(Reg.LYC)) {
            regF.setBit(Reg.STAT, STATBits.LYC_EQ_LY, true);
            if (regF.testBit(Reg.STAT, STATBits.INT_LYC)) {
                cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);
            }
        } else {
            regF.setBit(Reg.STAT, STATBits.LYC_EQ_LY, false);
        }

    }

    private boolean isInRegistersBounds(int address) {
        return address >= AddressMap.REGS_LCDC_START
                && address < AddressMap.REGS_LCDC_END;
    }

    private boolean isInVideoRamBounds(int address) {
        return address >= AddressMap.VIDEO_RAM_START
                && address < AddressMap.VIDEO_RAM_END;
    }

    private boolean isInOAMBounds(int address) {
        return address >= AddressMap.OAM_START && address < AddressMap.OAM_END;
    }

}
