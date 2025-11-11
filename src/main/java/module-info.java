module library {
	requires javafx.controls;
	requires javafx.fxml;
	requires mapdb;
	requires annotations;
	requires static lombok;
	requires org.eclipse.collections.api;

	opens library;
	exports library;

	opens library.controllers;
	exports library.controllers;
}
