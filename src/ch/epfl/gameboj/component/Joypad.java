package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;

public class Joypad implements Component {

    public enum Key {

        RIGHT(0, 0), LEFT(0, 1), UP(0, 2), DOWN(0, 3), A(1, 0), B(1,
                1), SELECT(1, 2), START(1, 3);

        public final int lineIndex, columnIndex;

        private Key(int lineIndex, int columnIndex) {
            this.lineIndex = lineIndex;
            this.columnIndex = columnIndex;
        }

    }

    private static final int line0Bit = 4;
    private static final int line1Bit = 5;

    private Cpu cpu;

    private int[] pressedKeys = new int[2];
    private int regP1 = 0;

    public Joypad(Cpu cpu) {
        this.cpu = cpu;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (address == AddressMap.REG_P1) {
            return Bits.complement8(regP1);
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int value) {
        Preconditions.checkBits8(value);
        Preconditions.checkBits16(address);
        if (address == AddressMap.REG_P1) {
            int invertedValue = Bits.complement8(value);
            int oldColumns = getColumnState();
            regP1 = (Bits.extract(invertedValue, 4, 2) << 4) | oldColumns;
            updateColumns();
            checkUpdate(oldColumns);
        }
    }

    public void keyPressed(Key k) {
        int oldColumns = getColumnState();
        setBitTo(k, true);
        checkUpdate(oldColumns);
    }

    public void keyReleased(Key k) {
        setBitTo(k, false);
    }

    private void setBitTo(Key k, boolean newValue) {
        pressedKeys[k.lineIndex] = Bits.set(pressedKeys[k.lineIndex],
                k.columnIndex, newValue);
        updateColumns();
    }

    private void updateColumns() {
        int columnsFromLine0 = Bits.test(regP1, line0Bit) ? pressedKeys[0] : 0;
        int columnsFromLine1 = Bits.test(regP1, line1Bit) ? pressedKeys[1] : 0;
        int finalColumns = columnsFromLine0 | columnsFromLine1;
        regP1 = (Bits.extract(regP1, 4, 2) << 4) | finalColumns;
    }

    private void checkUpdate(int oldColumns) {
        int newColumnState = getColumnState();
        if (((~oldColumns) & newColumnState) != 0)
            cpu.requestInterrupt(Cpu.Interrupt.JOYPAD);
    }

    private int getColumnState() {
        return Bits.clip(4, regP1);
    }

}
