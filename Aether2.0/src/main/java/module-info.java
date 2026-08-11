module com.example.demoaether {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    requires org.orekit;
    requires hipparchus.geometry;
    requires hipparchus.ode;


    opens com.example.demoaether to javafx.fxml;
    exports com.example.demoaether;
}
