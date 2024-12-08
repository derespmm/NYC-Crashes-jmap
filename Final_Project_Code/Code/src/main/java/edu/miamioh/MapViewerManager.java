package edu.miamioh;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.DefaultMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

public class MapViewerManager {

    private JMapViewer mapViewer;
    private final DatabaseManager databaseManager;

    public MapViewerManager() {
        // Initialize DatabaseManager instance
        databaseManager = new DatabaseManager();
        databaseManager.setupDataSource();
    }

    public void displayMap(ResultSet resultSet, JPanel pane) throws SQLException {
        // Create the map viewer
        mapViewer = new JMapViewer();
        mapViewer.setTileSource(new OsmTileSource.Mapnik());
        mapViewer.setPreferredSize(new Dimension(800, 800));

        // Set initial map center
        if (resultSet.first()) {
            double lat = resultSet.getDouble("latitude");
            double lon = resultSet.getDouble("longitude");
            mapViewer.setDisplayPosition(new Coordinate(lat, lon), 10);
        }
        resultSet.beforeFirst(); // Reset the cursor back to the beginning

        // Add the custom map controller
        new CustomMapController(mapViewer, databaseManager);

        // Create the filter panel
        JPanel filterPanel = createFilterPanel();

        // Create a split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapViewer, filterPanel);
        splitPane.setDividerLocation(900); // Adjust initial divider location
        splitPane.setOneTouchExpandable(true);

        // Add the split pane to the provided pane (no new JFrame needed)
        pane.add(splitPane);
    }

    private JPanel createFilterPanel() throws SQLException {
        // Create a panel for the filters
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical stacking
        filterPanel.setPreferredSize(new Dimension(300, 800));

        // Plot Favorites button
        JButton plotFavoritesButton = new JButton("Plot Favorites");
        plotFavoritesButton.addActionListener(e -> plotFavorites());
        plotFavoritesButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, plotFavoritesButton.getPreferredSize().height));
        filterPanel.add(plotFavoritesButton);
    
        // Borough selection
        JLabel boroughLabel = new JLabel("Boroughs (comma separated):");
        JTextField boroughField = new JTextField();
        filterPanel.add(boroughLabel);
        filterPanel.add(boroughField);
    
        // Car type selection
        JLabel carTypeLabel = new JLabel("Car Types (comma separated):");
        JTextField carTypeField = new JTextField();
        filterPanel.add(carTypeLabel);
        filterPanel.add(carTypeField);
    
        // Date input (combined field for MM/DD/YYYY, comma separated)
        JLabel dateLabel = new JLabel("Between Dates (YYYY-MM-DD, comma separated):");
        JTextField dateField = new JTextField();
        filterPanel.add(dateLabel);
        filterPanel.add(dateField);

        // Apply Filter button
        JButton applyButton = new JButton("Apply Filter");
        applyButton.addActionListener(e -> plotPoints(boroughField, carTypeField, dateField));
        applyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, applyButton.getPreferredSize().height));
        filterPanel.add(applyButton); // Adding it below Plot Favorites button
    
        return filterPanel;
    }
    
    

    private void plotPoints(JTextField boroughField, JTextField carTypeField, JTextField dateField) {
        String boroughInput = boroughField.getText().trim();
        String carTypeInput = carTypeField.getText().trim();
        String dateInput = dateField.getText().trim();
    
        boroughInput = boroughInput.isEmpty() ? null : boroughInput.toLowerCase();
        carTypeInput = carTypeInput.isEmpty() ? null : carTypeInput.toLowerCase();
        dateInput = dateInput.isEmpty() ? null : dateInput;
    
        String[] boroughs = (boroughInput != null) ? boroughInput.split(",") : null;
        String[] carTypes = (carTypeInput != null) ? carTypeInput.split(",") : null;
        String[] dates = (dateInput != null) ? dateInput.split(",") : null;
    
        // Validate input dates directly
        String startDate = null, endDate = null;
        if (dates != null && dates.length == 2) {
            String date1 = dates[0].trim();
            String date2 = dates[1].trim();
        
            if (isValidDate(date1) && isValidDate(date2)) {
                startDate = date1;
                endDate = date2;
            } else {
                System.err.println("Invalid date format. Please use YYYY-MM-DD.");
                return; // Exit without applying the filter
            }
        }
    
        StringBuilder queryBuilder = new StringBuilder("SELECT c.latitude, c.longitude, c.collision_id FROM collision c ")
                .append("JOIN vehicletype v ON c.collision_id = v.collision_id WHERE 1=1");
    
        if (boroughs != null && boroughs.length > 0) {
            queryBuilder.append(" AND ").append(buildInClause("LOWER(c.borough)", boroughs));
        }
    
        if (carTypes != null && carTypes.length > 0) {
            queryBuilder.append(" AND ").append(buildInClause("LOWER(v.vehicle_type)", carTypes));
        }
    
        if (startDate != null && endDate != null) {
            queryBuilder.append(" AND c.crash_date BETWEEN '")
                        .append(startDate)
                        .append("' AND '")
                        .append(endDate)
                        .append("'");
        }
    
        try {
            ResultSet filteredResults = databaseManager.executeQuery(queryBuilder.toString());
            mapViewer.removeAllMapMarkers();
            while (filteredResults.next()) {
                double lat = filteredResults.getDouble("latitude");
                double lon = filteredResults.getDouble("longitude");
                int collisionId = filteredResults.getInt("collision_id");
                addMapMarkerWithClickEvent(lat, lon, collisionId);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isValidDate(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }


    private void addMapMarkerWithClickEvent(double lat, double lon, int collisionId) {
        MapMarkerDotWithCollisionId marker = new MapMarkerDotWithCollisionId(lat, lon, collisionId);
        mapViewer.addMapMarker(marker);
    }
    
    private static class MapMarkerDotWithCollisionId extends MapMarkerDot {
        private final int collisionId;
    
        public MapMarkerDotWithCollisionId(double lat, double lon, int collisionId) {
            super(lat, lon);
            this.collisionId = collisionId;
        }
    
        public int getCollisionId() {
            return collisionId;
        }
    }
    

    private String buildInClause(String columnName, String[] values) {
        StringBuilder clause = new StringBuilder(columnName + " IN (");
        for (int i = 0; i < values.length; i++) {
            clause.append("'").append(values[i].trim().toLowerCase()).append("'"); 
            if (i < values.length - 1) {
                clause.append(", ");
            }
        }
        clause.append(")");
        return clause.toString();
    }

    private void plotFavorites() {
        // Query to fetch all favorite collision IDs
        String query = "SELECT collision_id FROM favorites";
        
        try {
            ResultSet favoritesResult = databaseManager.executeQuery(query);
            mapViewer.removeAllMapMarkers(); // Clear existing markers
    
            while (favoritesResult.next()) {
                int collisionId = favoritesResult.getInt("collision_id");
    
                // Fetch detailed information for each favorite collision
                String detailsQuery = "SELECT latitude, longitude FROM collision WHERE collision_id = " + collisionId;
                ResultSet collisionDetails = databaseManager.executeQuery(detailsQuery);
    
                while (collisionDetails.next()) {
                    double lat = collisionDetails.getDouble("latitude");
                    double lon = collisionDetails.getDouble("longitude");
                    addMapMarkerWithClickEvent(lat, lon, collisionId);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Custom map controller class to handle mouse clicks
    private static class CustomMapController extends DefaultMapController {
        private final JMapViewer mapViewer;
        private final DatabaseManager databaseManager;
    
        public CustomMapController(JMapViewer mapViewer, DatabaseManager databaseManager) {
            super(mapViewer);
            this.mapViewer = mapViewer;
            this.databaseManager = databaseManager;
        }
    
        @Override
        public void mouseClicked(MouseEvent e) {
            // Get the click point
            Coordinate clickPosition = (Coordinate) mapViewer.getPosition(e.getPoint());
    
            // Check if the click is on a MapMarker
            for (MapMarker marker : mapViewer.getMapMarkerList()) {
                if (marker instanceof MapMarkerDotWithCollisionId) {
                    MapMarkerDotWithCollisionId dot = (MapMarkerDotWithCollisionId) marker;
                    if (isPointOnMarker(e.getPoint(), dot)) {
                        // Handle marker click and retrieve collision_id
                        int collisionId = dot.getCollisionId();
                        fetchCollisionInfo(collisionId);
                        return; // Exit loop after handling the click
                    }
                }
            }
        }

        private boolean isPointOnMarker(java.awt.Point point, MapMarkerDotWithCollisionId marker) {
            // Convert marker's coordinates to screen position
            java.awt.Point markerPosition = mapViewer.getMapPosition(marker.getCoordinate(), false);
            if (markerPosition == null) return false;
    
            // Define a small radius to detect clicks near the marker
            int radius = 5; // Adjust as necessary based on your UI
            return point.distance(markerPosition) <= radius;
        }

        class CollisionInfo {
            String collisionId;
            String crashDate;
            String crashTime;
            String borough;
            String zipCode;
            double latitude;
            double longitude;
            String onStreetName;
            String crossStreetName;
            String offStreetName;
            int numPerInj;
            int numPerKil;
            int numPedInj;
            int numPedKil;
            int numCycInj;
            int numCycKil;
            int numMotInj;
            int numMotKil;
            Set<String> contributingFactors = new HashSet<>();
            Set<String> vehicleTypes = new HashSet<>();
        }

        private void displayCollisionDetailsDialog(CollisionInfo collisionInfo) {
            JDialog dialog = new JDialog();
            dialog.setTitle("Collision Details");
            dialog.setSize(400, 380);
            dialog.setModal(true);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            panel.add(new JLabel("Collision ID: " + collisionInfo.collisionId));
            panel.add(new JLabel("Date: " + collisionInfo.crashDate));
            panel.add(new JLabel("Time: " + collisionInfo.crashTime));
            panel.add(new JLabel("Borough: " + collisionInfo.borough));
            panel.add(new JLabel("Zip Code: " + collisionInfo.zipCode));
            panel.add(new JLabel("Latitude: " + collisionInfo.latitude));
            panel.add(new JLabel("Longitude: " + collisionInfo.longitude));
            panel.add(new JLabel("On Street Name: " + collisionInfo.onStreetName));
            panel.add(new JLabel("Cross Street Name: " + collisionInfo.crossStreetName));
            panel.add(new JLabel("Off Street Name: " + collisionInfo.offStreetName));
            panel.add(new JLabel("Number of Persons Injured: " + collisionInfo.numPerInj));
            panel.add(new JLabel("Number of Persons Killed: " + collisionInfo.numPerKil));
            panel.add(new JLabel("Number of Pedestrians Injured: " + collisionInfo.numPedInj));
            panel.add(new JLabel("Number of Pedestrians Killed: " + collisionInfo.numPedKil));
            panel.add(new JLabel("Number of Cyclists Injured: " + collisionInfo.numCycInj));
            panel.add(new JLabel("Number of Cyclists Killed: " + collisionInfo.numCycKil));
            panel.add(new JLabel("Number of Motorists Injured: " + collisionInfo.numMotInj));
            panel.add(new JLabel("Number of Motorists Killed: " + collisionInfo.numMotKil));
            panel.add(new JLabel("Vehicle Types: " + collisionInfo.vehicleTypes.toString()));
            panel.add(new JLabel("Contributing Factors: " + collisionInfo.contributingFactors.toString()));

            JButton favoriteButton = new JButton("Add to Favorites");
            favoriteButton.addActionListener(e -> {
                addToFavorites(collisionInfo);
                // Here you can add logic to store the favorite in a database or a local file
            });
            panel.add(favoriteButton);

            dialog.add(panel);
            dialog.setVisible(true);
        }

        private void addToFavorites(CollisionInfo collisionInfo) {
            if (isFavorite(collisionInfo)) {
                System.out.println("This collision is already a favorite.");
                return;
            }

            String query = "INSERT INTO favorites (collision_id) VALUES (" + collisionInfo.collisionId + ");";
            
            try {
                databaseManager.executeInsert(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            System.out.println("Collision added to favorites: " + collisionInfo.collisionId);
        }

        private boolean isFavorite(CollisionInfo collisionInfo) {
            String query = "SELECT * FROM favorites WHERE collision_id = " + collisionInfo.collisionId + ";";

            try {
                ResultSet resultSet = databaseManager.executeQuery(query);

                if (resultSet.next()) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }
        

        private void fetchCollisionInfo(int collisionId) {
            // SQL query to fetch all collision details, including contributing factors and vehicle type
            String query = "SELECT * "
                    + "FROM collision c "
                    + "JOIN contributingfactors cf ON c.collision_id = cf.collision_id "
                    + "JOIN vehicletype vt ON c.collision_id = vt.collision_id "
                    + "WHERE c.collision_id = " + collisionId + ";";

            CollisionInfo collisionInfo = new CollisionInfo();

            try {
                ResultSet resultSet = databaseManager.executeQuery(query);

                while (resultSet.next()) {
                    if (collisionInfo.collisionId == null) {
                        collisionInfo.collisionId = resultSet.getString("collision_id");
                        collisionInfo.crashDate = resultSet.getString("crash_date");
                        collisionInfo.crashTime = resultSet.getString("crash_time");
                        collisionInfo.borough = resultSet.getString("borough");
                        collisionInfo.zipCode = resultSet.getString("zip_code");
                        collisionInfo.latitude = resultSet.getDouble("latitude");
                        collisionInfo.longitude = resultSet.getDouble("longitude");
                        collisionInfo.onStreetName = resultSet.getString("on_street_name");
                        collisionInfo.crossStreetName = resultSet.getString("cross_street_name");
                        collisionInfo.offStreetName = resultSet.getString("off_street_name");
                        collisionInfo.numPerInj = resultSet.getInt("number_of_persons_injured");
                        collisionInfo.numPerKil = resultSet.getInt("number_of_persons_killed");
                        collisionInfo.numPedInj = resultSet.getInt("number_of_pedestrians_injured");
                        collisionInfo.numPedKil = resultSet.getInt("number_of_pedestrians_killed");
                        collisionInfo.numCycInj = resultSet.getInt("number_of_cyclists_injured");
                        collisionInfo.numCycKil = resultSet.getInt("number_of_cyclists_killed");
                        collisionInfo.numMotInj = resultSet.getInt("number_of_motorists_injured");
                        collisionInfo.numMotKil = resultSet.getInt("number_of_motorists_killed");
                    }
                    collisionInfo.contributingFactors.add(resultSet.getString("factor_vehicle"));
                    collisionInfo.vehicleTypes.add(resultSet.getString("vehicle_type"));
                }
                displayCollisionDetailsDialog(collisionInfo);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
