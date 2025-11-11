module library {
	requires javafx.controls;
	requires javafx.fxml;

	opens library;
	exports library;

	opens library.controllers;
	exports library.controllers;
}
