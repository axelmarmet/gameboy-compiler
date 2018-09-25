package ch.epfl.gameboj.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.List;

import ch.epfl.bonus.compiler.Compiler;
import ch.epfl.bonus.language.GameboyLanguageException;
import ch.epfl.bonus.parser.Expression;
import ch.epfl.bonus.parser.Parser;
import ch.epfl.bonus.scanner.Scanner;
import ch.epfl.bonus.scanner.Token;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	private File saveFile;
	// Ugly but server does not accept save file
	private final static String initialSource = "main() { \n" + "\t var n = 0 \n" + "\t while(n < 8 ){ \n"
			+ "\t\t output fib(n) \n" + "\t\t n = n + 1 \n" + "\t } \n" + "} \n" + "fib(n){ \n" + "\t if(n == 0){ \n"
			+ "\t\t return 0 \n" + "\t } else if (n == 1) { \n" + "\t\t return 1 \n" + "\t } else { \n"
			+ "\t\t return fib(n-1) + fib(n-2) \n" + "\t}\n" + "}";
	private String code;
	private SimpleObjectProperty<String> consoleProperty = new SimpleObjectProperty<>();
	private SimpleObjectProperty<String> registerProperty = new SimpleObjectProperty<>();
	private GameBoy gameboy;

	public static void main(String[] args) {
		if (args.length != 1)
			throw new IllegalArgumentException("Expected one argument to locate the saved file");
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		initSaveFile();
		BorderPane borderPane = new BorderPane();
		BorderPane registerArea = new BorderPane();

		Button compileButton = new Button("Compile");
		compileButton.setOnAction((e) -> {
			try {
				compileCode();
				runCode();
			} catch (Exception exception) {
				consoleProperty.unbind();
				consoleProperty.set(exception.getMessage());
			}
		});

		Button debuggingButton = new Button("Debugging Mode");
		Button exitButton = new Button("Exit Debugging Mode");
		Button stepButton = new Button("Step");

		stepButton.setVisible(false);
		exitButton.setVisible(false);

		debuggingButton.setOnAction((e) -> {
			stepButton.setVisible(true);
			exitButton.setVisible(true);
			registerArea.setManaged(true);
			registerArea.setVisible(true);
			debuggingButton.setVisible(false);
			debuggingButton.setManaged(false);
			compileButton.setVisible(false);
			compileButton.setManaged(false);
			try {
				compileCode();
				createGameboy();
				registerProperty.bind(gameboy.cpu().cpuProperty);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		stepButton.setOnAction((e) -> {
			gameboy.runOneCpuInstruction();
		});

		exitButton.setOnAction((e) -> {
			stepButton.setVisible(false);
			exitButton.setVisible(false);
			registerArea.setManaged(false);
			registerArea.setVisible(false);
			debuggingButton.setVisible(true);
			debuggingButton.setManaged(true);
			compileButton.setVisible(true);
			compileButton.setManaged(true);
			registerProperty.unbind();
		});
		ToolBar toolBar = new ToolBar(compileButton, debuggingButton, stepButton, exitButton);

		borderPane.setTop(toolBar);

		SplitPane splitPane = new SplitPane();
		splitPane.setDividerPosition(0, 0.5);

		TextArea codeArea = new TextArea(getSavedText());
		codeArea.textProperty().addListener((o, oV, nV) -> {
			code = nV;
		});
		TextArea registerText = new TextArea();
		registerText.textProperty().bind(registerProperty);

		registerArea.setMinSize(200, 200);
		registerArea.setCenter(registerText);
		registerArea.setVisible(false);
		registerArea.setManaged(false);

		TextArea consoleText = new TextArea();
		consoleText.textProperty().bind(consoleProperty);
		BorderPane consoleArea = new BorderPane();
		consoleArea.setMinSize(200, 200);
		consoleArea.setCenter(consoleText);

		splitPane.getItems().addAll(codeArea, consoleArea);
		borderPane.setCenter(splitPane);
		borderPane.setRight(registerArea);

		primaryStage.setScene(new Scene(borderPane));
		primaryStage.show();
	}

	@Override
	public void stop() throws FileNotFoundException, IOException {
		saveText();
	}

	private void compileCode() throws IOException, GameboyLanguageException {

		Scanner scanner = new Scanner(code);
		List<Token> tokens = scanner.getTokens();

		Parser parser = new Parser(tokens);
		List<Expression> expressions = parser.parse();

		Compiler compiler = new Compiler();
		compiler.compile(expressions);
	}

	private void runCode() throws IOException {
		createGameboy();
		gameboy.runUntilCpuHalted();
	}

	private void createGameboy() throws IOException {
		consoleProperty.unbind();
		gameboy = new GameBoy(Cartridge.ofFile(new File("save.gb")));
		consoleProperty.bind(gameboy.serialPortPrintComponent().consoleProperty);
	}

	private void initSaveFile() throws IOException {
		String path = getParameters().getRaw().get(0);
		saveFile = new File(path);
	}

	private String getSavedText() throws FileNotFoundException, IOException {
		if (!saveFile.exists()) {
			saveFile.createNewFile();
			code = initialSource;
			return initialSource;
		}
		List<String> lines = Files.readAllLines(saveFile.toPath());
		StringBuilder sb = new StringBuilder();
		lines.forEach((x) -> {
			sb.append(x).append('\n');
		});
		code = sb.toString();
		return code;
	}

	private void saveText() throws FileNotFoundException, IOException {
		try (Writer writer = new PrintWriter(saveFile)) {
			writer.write(code);
		}
	}

}
