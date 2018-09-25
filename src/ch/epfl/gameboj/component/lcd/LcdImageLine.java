package ch.epfl.gameboj.component.lcd;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class LcdImageLine {

    // Used to reduce the memory footprint of initialising more than one empty
    // line in the program
    public static final LcdImageLine EMPTY_LCD_WIDTH_LINE = new LcdImageLine(
            BitVector.ZERO_BITVECTOR_LCD_WIDTH,
            BitVector.ZERO_BITVECTOR_LCD_WIDTH,
            BitVector.ZERO_BITVECTOR_LCD_WIDTH);

    private static final int NO_CHANGE_MAP = 0b11100100;

    private final BitVector msb;
    private final BitVector lsb;
    private final BitVector opacity;

    /**
     * Creates a new LcdImageLine with the given BitVectors
     * 
     * @param msb
     *            The BitVector containing the msb for the colour of each pixel
     *            (must not be null)
     * @param lsb
     *            The BitVector containing the lsb for the colour of each pixel
     *            (must not be null)
     * @param opacity
     *            The BitVector containing the opacity for the colour of each
     *            pixel (must not be null)
     * @throws NullPointerException
     *             If any of the given BitVectors are null
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
        // This precondition also checks that no BitVector is equal to null
        Preconditions.checkArgument(
                msb.size() == lsb.size() && lsb.size() == opacity.size());
        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LcdImageLine) {
            return msb.equals(((LcdImageLine) other).msb)
                    && lsb.equals(((LcdImageLine) other).lsb)
                    && opacity.equals(((LcdImageLine) other).opacity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(msb, lsb, opacity);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("msb     : ").append(msb);
        sb.append("\nlsb     : ").append(lsb);
        sb.append("\nopacity : ").append(opacity);
        return sb.toString();
    }

    /**
     * Returns the size of the LcdImageLine
     * 
     * @return the size of the LcdImageLine
     */
    public int size() {
        return msb.size();
    }

    /**
     * Returns the msb BitVector of the LcdImageLine
     * 
     * @return the msb BitVector of the LcdImageLine
     */
    public BitVector getMSB() {
        return msb;
    }

    /**
     * Returns the lsb BitVector of the LcdImageLine
     * 
     * @return the lsb BitVector of the LcdImageLine
     */
    public BitVector getLSB() {
        return lsb;
    }

    /**
     * Returns the opacity BitVector of the LcdImageLine
     * 
     * @return the opactiy BitVector of the LcdImageLine
     */
    public BitVector getOpacity() {
        return opacity;
    }

    /**
     * Returns a new LcdImageLine containing this one shifted by the given
     * distance
     * 
     * @param distance
     *            the distance of the shift, if negative the line will be
     *            shifted to the right, otherwise to the left
     * @return a new LcdImageLine containing this one shifted by the given
     *         distance
     */
    public LcdImageLine shift(int distance) {
        return new LcdImageLine(msb.shift(distance), lsb.shift(distance),
                opacity.shift(distance));
    }

    /**
     * Returns a new LcdImageLine containing the given wrapped extraction
     * 
     * @param start
     *            the start of the wrapped extraction
     * @param size
     *            the size of the wrapped extraction (must be divisible by 32
     *            and strictly bigger than zero)
     * @throws IllegalArgumentException
     *             if size is not divisible by 32 or not strictly bigger than 0
     * @return a new LcdImageLine containing the given wrapped extraction
     */
    public LcdImageLine extractWrapped(int start, int size) {
        // The preconditions are checked by the BitVector themselves
        return new LcdImageLine(msb.extractWrapped(start, size),
                lsb.extractWrapped(start, size),
                opacity.extractWrapped(start, size));
    }

    /**
     * Returns a new LcdImageLine containing this LcdImageLine to which the
     * given palette has been applied
     * 
     * @param palette
     *            The palette that will be used (must fit in a byte)
     * @throws IllegalArgumentException
     *             if the given palette does not fit in a byte
     * @return a new LcdImageLine containing this LcdImageLine to which the
     *         given palette has been applied
     */
    public LcdImageLine mapColors(int palette) {
        Preconditions.checkBits8(palette);
        if (palette == NO_CHANGE_MAP) {
            return this;
        }

        BitVector newMSB = new BitVector(size());
        BitVector newLSB = new BitVector(size());

        BitVector inversedMSB = msb.not();
        BitVector inversedLSB = lsb.not();

        BitVector[] colorMasks = {
                // Color 0
                inversedMSB.and(inversedLSB),
                // Color 1
                inversedMSB.and(lsb),
                // Color 2
                msb.and(inversedLSB),
                // Color 3
                msb.and(lsb) };

        for (int i = 0; i < 4; ++i) {
            if (Bits.test(palette, i * 2))
                newLSB = newLSB.or(colorMasks[i]);
            if (Bits.test(palette, i * 2 + 1))
                newMSB = newMSB.or(colorMasks[i]);
        }

        return new LcdImageLine(newMSB, newLSB, opacity);
    }

    /**
     * Returns a new LcdImageLine whose first n pixels are from this
     * LcdImageLine and the other from the given other LcdImageLine
     * 
     * @param other
     *            The other LcdImageLoin that will be used (must not be null and
     *            of same size as this LcdImageLine)
     * @param n
     *            The amount of pixel from this LcdImageLine used (must be
     *            between 0 (inclusive) and the size of LcdImageLine
     *            (exclusive))
     * @throws IllegalArgumentException
     *             if other is not the same size as this LcdImageLine
     * @throws NullPointerException
     *             if other is null
     * @throws IndexOutOfBoundsException
     *             if n is not between 0 (inclusive) and the size of
     *             LcdImageLine (exclusive)
     * @return a new LcdImageLine whose first n pixels are from this
     *         LcdImageLine and the other from the given other LcdImageLine
     */
    public LcdImageLine join(LcdImageLine other, int n) {
        Preconditions.checkArgument(other.size() == size());
        Objects.checkIndex(n, size());
        BitVector mask = new BitVector(size(), true).shift(n - size());

        LcdImageLine contributionFromThis = new LcdImageLine(msb.and(mask),
                lsb.and(mask), opacity.and(mask));
        LcdImageLine contributionFromOther = other.shift(n);

        return new LcdImageLine(
                contributionFromThis.msb.or(contributionFromOther.msb),
                contributionFromThis.lsb.or(contributionFromOther.lsb),
                contributionFromThis.opacity.or(contributionFromOther.opacity));
    }

    /**
     * Return a new LcdImageLine whose pixel are those of the given top
     * LcdImageLine if they are opaque otherwise the pixels are from this
     * LcdImageLine
     * 
     * @param top
     *            The LcdImageLine that will be used as the top (must not be
     *            null)
     * @throws NullPointerException
     *             if the given top is null
     * @return a new LcdImageLine whose pixel are those of the given top
     *         LcdImageLine if they are opaque otherwise the pixels are from
     *         this LcdImageLine
     */
    public LcdImageLine below(LcdImageLine top) {
        return below(top, top.opacity);
    }

    /**
     * Returns a new LcdImageLine whose pixels are those of the given top
     * LcdImageLine if the bit of the given opacity is 1 for the given pixel and
     * of this LcdImageLine if it is 0
     * 
     * @param top
     *            The LcdImageLine that will be used as the top (must not be
     *            null)
     * @param opacity
     *            the opacity that will be used in the calculation (must not be
     *            null)
     * @throws NullPointerException
     *             if the given top or opacity vector is null
     * @return A new LcdImageLine whose pixels are those of the given top
     *         LcdImageLine if the bit of the given opacity is 1 for the given
     *         pixel and of this LcdImageLine if it is 0
     */
    public LcdImageLine below(LcdImageLine top, BitVector opacity) {
        BitVector inversedOpacity = opacity.not();

        BitVector msbFromTop = top.msb.and(opacity);
        BitVector msbFromBottom = msb.and(inversedOpacity);

        BitVector lsbFromTop = top.lsb.and(opacity);
        BitVector lsbFromBottom = lsb.and(inversedOpacity);

        return new LcdImageLine(msbFromBottom.or(msbFromTop),
                lsbFromBottom.or(lsbFromTop), opacity.or(this.opacity));
    }

    static public final class Builder {

        // NOTE : no need to check that this builder builds only one object as
        // the inner BitVector Builders already do this check

        private final BitVector.Builder msbBuilder;
        private final BitVector.Builder lsbBuilder;

        /**
         * Creates a new LcdImageLine Builder of the given size
         * 
         * @param size
         *            the size of the created Builder (must be strictly bigger
         *            than 0 and divisible by 32)
         * @throws IllegalArgumentException
         *             if the given size is not strictly bigger than 0 or not
         *             divisible by 32
         */
        public Builder(int size) {
            // The preconditions are checked by the BitVector Builder themselves
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
        }

        /**
         * Sets the byte of given index of the msb Bitvector to the given
         * msbValue and the byte of given index of the lsb BitVector to the
         * given lsbValue
         * 
         * @param msbValue
         *            the new value of the byte of given index of the msb
         *            BitVector (must fit in a byte)
         * @param lsbValue
         *            the new value of the byte of given index of the lsb
         *            BitVector (must fit in a byte)
         * @param index
         *            the index of the bytes to set (must be between 0
         *            (inclusive) and the size of the builder (exclusive))
         * @throws IllegalArgumentException
         *             if msbValue or lsbValue do not fit a byte
         * @throws IndexOutOfBoundsException
         *             if the given index is not between 0 (inclusive) and the
         *             size of the builder (exclusive)
         * @throws IllegalStateException
         *             if an object has already been built with this builder
         * @return The builder after the bytes have been set
         */
        public Builder setBytes(int msbValue, int lsbValue, int index) {
            // The preconditions are checked by the BitVector Builder themselves
            msbBuilder.setByte(msbValue, index);
            lsbBuilder.setByte(lsbValue, index);
            return this;
        }

        /**
         * Returns the built LcdImageLine stored in this builder. This builder
         * cannot create anymore object after this function has been called
         * 
         * @throws IllegalStateException
         *             if an object has already been built with this builder
         * @return the built LcdImageLine stored in this builder. This builder
         *         cannot create anymore object after this function has been
         *         called
         */
        public LcdImageLine build() {
            // The preconditions are checked by the BitVector Builder themselves
            BitVector msb = msbBuilder.build();
            BitVector lsb = lsbBuilder.build();
            return new LcdImageLine(msb, lsb, msb.or(lsb));
        }
    }
}
