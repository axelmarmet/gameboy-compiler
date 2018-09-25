package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class LcdImage {

    private final int width, height;
    private final List<LcdImageLine> lines;

    /**
     * Creates a new LcdImage having for lines the given List lines
     * 
     * @param lines
     *            the lines of the new LcdImage (must no be null or empty)
     * @throws IllegalArgumentException
     *             if the given List lines is empty
     * @throws NullPointerException
     *             if the given List lines is null
     * 
     */
    public LcdImage(List<LcdImageLine> lines) {
        // Note : Chose to remove the argument width and height from the
        // constructor as they can be inferred from the given list and brought
        // nothing to the table except more precondition checks
        // Checks at the same time that the given list is not null and not empty
        Preconditions.checkArgument(!lines.isEmpty());
        this.lines = lines;
        this.width = lines.get(0).size();
        this.height = lines.size();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LcdImage)
            return lines.equals(((LcdImage) other).lines);
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        lines.forEach(sb::append);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return lines.hashCode();
    }

    /**
     * Returns the width of the LcdImage
     * 
     * @return the width of the LcdImage
     */
    public int width() {
        return width;
    }

    /**
     * Returns the height of the LcdImage
     * 
     * @return the height of the LcdImage
     */
    public int height() {
        return height;
    }

    /**
     * Returns the colour of the pixel at coordinates (x,y)
     * 
     * @param x
     *            the x coordinate of the pixel (must be between 0 (inclusive)
     *            and the width of the image (exclusive))
     * @param y
     *            the y coordinate of the pixel (must be between 0 (inclusive)
     *            and the height of the image (exclusive))
     * @throws IndexOutOfBoundsException
     *             if x or y are out of bounds
     * @return the colour of the pixel at coordinates (x,y)
     */
    public int get(int x, int y) {
        // No need to test the indexes as it is done by the functions get() from
        // lines and testBit() from the BitVectors
        int color = lines.get(y).getLSB().testBit(x) ? 1 : 0;
        color += lines.get(y).getMSB().testBit(x) ? 2 : 0;
        return color;

    }

    static public final class Builder {

        private boolean hasBuilt;
        private final List<LcdImageLine> lines;
        private final int width;

        /**
         * Creates a new LcdImage Builder with the given width and height
         * 
         * @param width
         *            the width of the LcdImage being built (must be strictly
         *            bigger than zero)
         * @param height
         *            the height of the LcdImage being built (must be strictly
         *            bigger than zero)
         * @throws IllegalArgumentException
         *             if width or height are not strictly positive
         */
        public Builder(int width, int height) {
            Preconditions.checkArgument(width > 0 && height > 0);
            this.width = width;
            // Note : there is no need to store the height as it is only used to
            // check the index when using setLine and the List lines already
            // does it
            BitVector emptyBitVector = new BitVector(width);
            LcdImageLine emptyLine = new LcdImageLine(emptyBitVector,
                    emptyBitVector, emptyBitVector);
            lines = new ArrayList<LcdImageLine>(
                    Collections.nCopies(height, emptyLine));
            hasBuilt = false;
        }

        /**
         * Set the line of the given index to the given line
         * 
         * @param line
         *            the new line that will be used (must not be null and of
         *            the same size as the width of the LcdImage being built)
         * @param index
         *            the index of the line to set (must be between 0
         *            (inclusive) and the height of the LcdImage being built
         *            (exclusive))
         * @param NullPointerException
         *            if the given line is null
         * @throws IllegalArgumentException
         *             if the size of the line is not equal to the width of the
         *             builder
         * @throws IndexOutOfBoundsException
         *             if the given index is out of bounds
         * @throws IllegalStateException
         *             if an object has already been built with this builder
         * @return the builder after the line has been set
         */
        public Builder setLine(LcdImageLine line, int index) {
            Preconditions.checkArgument(line.size() == width);
            checkState();

            lines.set(index, line);
            return this;
        }

        /**
         * Build and return the LcdImage. This builder can not build another
         * object after this method has been called
         * 
         * @throws IllegalStateException
         *             if an object has already been built with this builder
         * @return the LcdImage being built
         */
        public LcdImage build() {
            checkState();
            hasBuilt = true;
            return new LcdImage(lines);
        }

        private void checkState() {
            if (hasBuilt)
                throw new IllegalStateException(
                        "An object has already been built with this builder");
        }
    }

}
