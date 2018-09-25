package ch.epfl.gameboj.component;

/**
 * @author Axel Marmet (288862)
 *
 */
public interface Clocked {

    /**
     * Ask the component implementing this method to execute all operations that
     * it must do in a cycle of the given index
     * 
     * @param cycle
     */
    public void cycle(long cycle);

}
