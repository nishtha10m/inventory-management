import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InventoryManagementSystem {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField nameField, quantityField, priceField, searchField;
    private JLabel totalValueLabel;
    private Map<String, Item> inventory;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    public InventoryManagementSystem() {
        inventory = new HashMap<>();
        initializeUI();
        updateTable(); // Just initialize with empty table instead of trying to load
    }

    private void initializeUI() {
        // Main frame setup
        frame = new JFrame("Inventory Management System");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // Create menu bar
        createMenuBar();

        // Top panel for search functionality
        JPanel topPanel = new JPanel(new BorderLayout());
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchInventory());

        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        // Status panel with total inventory value
        JPanel statusPanel = new JPanel();
        totalValueLabel = new JLabel("Total Inventory Value: " + currencyFormat.format(0));
        statusPanel.add(totalValueLabel);
        topPanel.add(statusPanel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);

        // Table setup
        tableModel = new DefaultTableModel(
                new String[]{"Item Name", "Quantity", "Price", "Total Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return Integer.class;
                if (columnIndex == 2 || columnIndex == 3) return Double.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);

        // Add row sorting capability
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(BorderFactory.createTitledBorder("Item Details"));
        inputPanel.setLayout(new GridLayout(4, 2, 5, 5));

        nameField = new JTextField();
        quantityField = new JTextField();
        priceField = new JTextField();

        inputPanel.add(new JLabel("Item Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(priceField);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton addButton = new JButton("Add Item");
        addButton.addActionListener(e -> addItem());

        JButton updateButton = new JButton("Update Selected");
        updateButton.addActionListener(e -> updateSelectedItem());

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedItem());

        JButton clearButton = new JButton("Clear Fields");
        clearButton.addActionListener(e -> clearFields());

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        inputPanel.add(new JLabel(""));
        inputPanel.add(buttonPanel);

        // Add listener to table selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                populateFieldsFromSelection();
            }
        });

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.CENTER);

        frame.add(southPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveMenuItem = new JMenuItem("Save Inventory");
        saveMenuItem.addActionListener(e -> saveInventory());

        JMenuItem loadMenuItem = new JMenuItem("Load Inventory");
        loadMenuItem.addActionListener(e -> loadInventoryWithFileChooser());

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Save before exiting?", "Exit Application",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                saveInventory();
                System.exit(0);
            } else if (confirm == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
        });

        fileMenu.add(saveMenuItem);
        fileMenu.add(loadMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // Report menu
        JMenu reportMenu = new JMenu("Reports");

        JMenuItem inventoryValueMenuItem = new JMenuItem("Inventory Value Report");
        inventoryValueMenuItem.addActionListener(e -> generateValueReport());

        JMenuItem lowStockMenuItem = new JMenuItem("Low Stock Report");
        lowStockMenuItem.addActionListener(e -> generateLowStockReport());

        reportMenu.add(inventoryValueMenuItem);
        reportMenu.add(lowStockMenuItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "Inventory Management System v1.0\n© 2025",
                "About", JOptionPane.INFORMATION_MESSAGE));

        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);
    }

    private void populateFieldsFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = table.convertRowIndexToModel(viewRow);
        String itemName = tableModel.getValueAt(modelRow, 0).toString();

        if (inventory.containsKey(itemName)) {
            Item item = inventory.get(itemName);
            nameField.setText(item.name);
            quantityField.setText(String.valueOf(item.quantity));
            priceField.setText(String.valueOf(item.price));
        }
    }

    private void addItem() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Item name cannot be empty", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                JOptionPane.showMessageDialog(frame, "Quantity cannot be negative", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) {
                JOptionPane.showMessageDialog(frame, "Price cannot be negative", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (inventory.containsKey(name)) {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Item already exists. Update quantity and price?",
                        "Confirm Update", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    Item existingItem = inventory.get(name);
                    existingItem.quantity += quantity;
                    existingItem.price = price;
                    JOptionPane.showMessageDialog(frame, "Item updated successfully!");
                }
            } else {
                inventory.put(name, new Item(name, quantity, price));
                JOptionPane.showMessageDialog(frame, "Item added successfully!");
            }

            updateTable();
            clearFields();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter valid numbers for quantity and price",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(frame, "Please select an item to update", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int modelRow = table.convertRowIndexToModel(viewRow);
            String itemName = tableModel.getValueAt(modelRow, 0).toString();

            String name = nameField.getText().trim();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            if (quantity < 0 || price < 0) {
                JOptionPane.showMessageDialog(frame, "Quantity and price cannot be negative",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // If name changed, we need to remove old item and add new one
            if (!itemName.equals(name)) {
                inventory.remove(itemName);
                inventory.put(name, new Item(name, quantity, price));
            } else {
                // Just update existing item
                Item item = inventory.get(itemName);
                item.quantity = quantity;
                item.price = price;
            }

            updateTable();
            clearFields();
            JOptionPane.showMessageDialog(frame, "Item updated successfully!");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter valid numbers for quantity and price",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(frame, "Please select an item to delete", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String itemName = tableModel.getValueAt(modelRow, 0).toString();

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to delete " + itemName + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            inventory.remove(itemName);
            updateTable();
            clearFields();
            JOptionPane.showMessageDialog(frame, "Item deleted successfully!");
        }
    }

    private void searchInventory() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            updateTable(); // Reset to show all items
            return;
        }

        tableModel.setRowCount(0);
        double totalValue = 0;

        for (Item item : inventory.values()) {
            if (item.name.toLowerCase().contains(searchTerm)) {
                double itemTotalValue = item.quantity * item.price;
                tableModel.addRow(new Object[]{
                        item.name,
                        item.quantity,
                        item.price,
                        itemTotalValue
                });
                totalValue += itemTotalValue;
            }
        }

        totalValueLabel.setText("Total Inventory Value: " + currencyFormat.format(totalValue));
    }

    private void clearFields() {
        nameField.setText("");
        quantityField.setText("");
        priceField.setText("");
        table.clearSelection();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        double totalValue = 0;

        for (Item item : inventory.values()) {
            double itemTotalValue = item.quantity * item.price;
            tableModel.addRow(new Object[]{
                    item.name,
                    item.quantity,
                    item.price,
                    itemTotalValue
            });
            totalValue += itemTotalValue;
        }

        totalValueLabel.setText("Total Inventory Value: " + currencyFormat.format(totalValue));
    }

    private void saveInventory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Inventory");

        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".inv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".inv");
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileToSave))) {
                ArrayList<Item> items = new ArrayList<>(inventory.values());
                oos.writeObject(items);
                JOptionPane.showMessageDialog(frame, "Inventory saved successfully to " + fileToSave.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error saving inventory: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Modified to always use file chooser instead of default location
    private void loadInventoryWithFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Inventory");

        int userSelection = fileChooser.showOpenDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            if (fileToLoad.exists()) {
                loadFromFile(fileToLoad);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "File not found: " + fileToLoad.getAbsolutePath(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(frame,
                    "File does not exist: " + file.getAbsolutePath(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ArrayList<Item> items = (ArrayList<Item>) ois.readObject();
            inventory.clear();

            for (Item item : items) {
                inventory.put(item.name, item);
            }

            updateTable();
            JOptionPane.showMessageDialog(frame, "Inventory loaded successfully from " + file.getAbsolutePath());

        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading inventory: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void generateValueReport() {
        JFrame reportFrame = new JFrame("Inventory Value Report");
        reportFrame.setSize(500, 400);
        reportFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        reportFrame.setLayout(new BorderLayout());

        DefaultTableModel reportModel = new DefaultTableModel(
                new String[]{"Item Name", "Quantity", "Unit Price", "Total Value"}, 0);

        JTable reportTable = new JTable(reportModel);
        JScrollPane scrollPane = new JScrollPane(reportTable);

        double grandTotal = 0;

        for (Item item : inventory.values()) {
            double totalValue = item.quantity * item.price;
            reportModel.addRow(new Object[]{
                    item.name,
                    item.quantity,
                    currencyFormat.format(item.price),
                    currencyFormat.format(totalValue)
            });
            grandTotal += totalValue;
        }

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.add(new JLabel("Grand Total: " + currencyFormat.format(grandTotal)));

        JButton printButton = new JButton("Print Report");
        printButton.addActionListener(e -> JOptionPane.showMessageDialog(reportFrame,
                "Printing functionality would be implemented here",
                "Print", JOptionPane.INFORMATION_MESSAGE));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(printButton);

        reportFrame.add(scrollPane, BorderLayout.CENTER);
        reportFrame.add(summaryPanel, BorderLayout.SOUTH);
        reportFrame.add(buttonPanel, BorderLayout.NORTH);

        reportFrame.setLocationRelativeTo(frame);
        reportFrame.setVisible(true);
    }

    private void generateLowStockReport() {
        int threshold = 10; // Default threshold

        String input = JOptionPane.showInputDialog(frame,
                "Enter quantity threshold for low stock items:",
                threshold);

        if (input != null) {
            try {
                threshold = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid input. Using default threshold of 10.");
            }
        } else {
            return; // User canceled
        }

        JFrame reportFrame = new JFrame("Low Stock Report (Quantity < " + threshold + ")");
        reportFrame.setSize(500, 400);
        reportFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        reportFrame.setLayout(new BorderLayout());

        DefaultTableModel reportModel = new DefaultTableModel(
                new String[]{"Item Name", "Current Quantity", "Reorder Suggested"}, 0);

        JTable reportTable = new JTable(reportModel);
        JScrollPane scrollPane = new JScrollPane(reportTable);

        int lowStockCount = 0;

        for (Item item : inventory.values()) {
            if (item.quantity < threshold) {
                reportModel.addRow(new Object[]{
                        item.name,
                        item.quantity,
                        "Yes"
                });
                lowStockCount++;
            }
        }

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryPanel.add(new JLabel("Total Low Stock Items: " + lowStockCount));

        reportFrame.add(scrollPane, BorderLayout.CENTER);
        reportFrame.add(summaryPanel, BorderLayout.SOUTH);

        reportFrame.setLocationRelativeTo(frame);
        reportFrame.setVisible(true);
    }

    static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int quantity;
        double price;

        public Item(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(InventoryManagementSystem::new);
    }
}
