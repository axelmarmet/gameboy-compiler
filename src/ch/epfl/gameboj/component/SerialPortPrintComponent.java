package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class SerialPortPrintComponent implements Component {

	public ObjectProperty<String> consoleProperty = new SimpleObjectProperty<>("");

	@Override
	public int read(int address) {
		return NO_DATA;
	}

	@Override
	public void write(int address, int data) {
		if (address == AddressMap.SERIAL_PORT)
			consoleProperty.setValue(consoleProperty.getValue() + data + '\n');
	}
}