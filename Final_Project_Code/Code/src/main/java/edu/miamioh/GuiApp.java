package edu.miamioh;

import java.awt.Container;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class GuiApp {

    private final DatabaseManager databaseManager;
    private final MapViewerManager mapViewerManager;
    private final Menubar menuBar;

    public GuiApp() {
        databaseManager = new DatabaseManager();
        mapViewerManager = new MapViewerManager();
        menuBar = new Menubar(); // Initialize MenuBar
    }

    public void componentManager(Container pane) throws SQLException {
        databaseManager.setupDataSource();
        ResultSet resultSet = databaseManager.fetchLocations();
        mapViewerManager.displayMap(resultSet, (JPanel) pane);
    }

    private void windowManager() {
        JFrame frame = new JFrame("NYC Collisions Database Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the MenuBar
        frame.setJMenuBar(menuBar.createMenuBar());

        try {
            componentManager(frame.getContentPane());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        GuiApp app = new GuiApp();
        app.windowManager();
    }
}
