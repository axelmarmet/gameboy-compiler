package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Timer implements Clocked, Component {

    private final static int MAX_SECOND_COUNTER_VALUE = 0xFF;

    private final Cpu cpu;
    private int mainCounter = 0;
    private int secondCounter = 0;
    private int secondCounterResetValue = 0;
    private int secondCounterConfig = 0;

    /**
     * Creates a new Timer with the given cpu attached
     * 
     * @param cpu
     *            the cpu that will be attached (must not be null)
     * @throws NullPointerException
     *             if cpu is null
     */
    public Timer(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        switch (address) {
        case AddressMap.REG_DIV:
            return Bits.extract(mainCounter, 8, 8);
        case AddressMap.REG_TIMA:
            return secondCounter;
        case AddressMap.REG_TMA:
            return secondCounterResetValue;
        case AddressMap.REG_TAC:
            return secondCounterConfig;
        default:
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        boolean state;
        switch (address) {
        case AddressMap.REG_DIV:
            state = currentState();
            mainCounter = 0;
            handleNewState(state);
            break;
        case AddressMap.REG_TIMA:
            secondCounter = data;
            break;
        case AddressMap.REG_TMA:
            secondCounterResetValue = data;
            break;
        case AddressMap.REG_TAC:
            state = currentState();
            secondCounterConfig = data;
            handleNewState(state);
            break;
        }
    }

    @Override
    public void cycle(long cycle) {
        boolean state = currentState();
        mainCounter = Bits.clip(16, mainCounter + 4);
        handleNewState(state);
    }

    private boolean currentState() {
        int bitToCheck;
        if (!Bits.test(secondCounterConfig, 2)) {
            return false;
        }
        switch (Bits.clip(2, secondCounterConfig)) {
        case 0b00:
            bitToCheck = 9;
            break;
        case 0b01:
            bitToCheck = 3;
            break;
        case 0b10:
            bitToCheck = 5;
            break;
        case 0b11:
            bitToCheck = 7;
            break;
        default:
            throw new IllegalArgumentException(
                    "Something went horribly wrong because clip did not work correctly");
        }
        return Bits.test(mainCounter, bitToCheck);

    }

    // Increment the second counter if the state changed from true to false and
    // request an interrupt if the secondTimer is at its max value and
    // incremented
    private void handleNewState(boolean oldState) {
        if (oldState && !currentState()) {
            if (secondCounter == MAX_SECOND_COUNTER_VALUE) {
                cpu.requestInterrupt(Interrupt.TIMER);
                secondCounter = secondCounterResetValue;
            } else {
                secondCounter++;
            }
        }
    }

}
