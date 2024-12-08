package edu.miamioh;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Menubar {

    // Create and return a JMenuBar instance
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create a 'File' menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Add actions to the File menu items
        exitItem.addActionListener(e -> System.exit(0));

        // Add the items to the file menu
        fileMenu.add(openItem);
        fileMenu.add(exitItem);

        // Add the 'File' menu to the menu bar
        menuBar.add(fileMenu);

        // Add any additional menus here if needed, e.g., Edit, Help
        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferencesItem = new JMenuItem("Preferences");
        editMenu.add(preferencesItem);
        menuBar.add(editMenu);

        return menuBar;
    }
}
