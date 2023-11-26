import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;

public class StockPortfolioApp {
    private JFrame frame;
    private JTextField tickerField, priceField, dateField, amountField;
    private DefaultTableModel tableModel;
    private JTable table;

    public StockPortfolioApp() {
        showInitialDialog();
    }

    private void showInitialDialog() {
        String[] options = {"Create New Portfolio", "Open Existing Portfolio"};
        int choice = JOptionPane.showOptionDialog(null, "Choose an option:", "Stock Portfolio",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            createNewPortfolio();
        } else if (choice == 1) {
            openExistingPortfolio();
        } else {
            System.exit(0); // Exit if the user cancels or closes the dialog
        }
    }

    private void createNewPortfolio() {
        initialize();
    }

    private void openExistingPortfolio() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
    
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Read file content and populate vectors
                Vector<Vector<String>> dataVector = new Vector<>();
                java.util.Scanner scanner = new java.util.Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] parts = line.split(",");
                    if (parts.length == 4) {
                        Vector<String> row = new Vector<>();
                        row.add(parts[0]);
                        row.add(parts[1]);
                        row.add(parts[2]);
                        row.add(parts[3]);
                        dataVector.add(row);
                    }
                }
                scanner.close();
    
                // Initialize the UI
                initialize();
    
                // Populate the table with the records
                for (Vector<String> row : dataVector) {
                    calculatePriceAndProfitLoss(row);

                    tableModel.addRow(row);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error reading file.");
            }
        } else {
            System.exit(0); // Exit if the user cancels or closes the dialog
        }
    }

    private void initialize() {
        frame = new JFrame("Stock Portfolio");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Input Section
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));

        tickerField = new JTextField(10);
        priceField = new JTextField(10);
        dateField = new JTextField(10);
        amountField = new JTextField(10);

        inputPanel.add(new JLabel("Ticker Symbol: "));
        inputPanel.add(tickerField);
        inputPanel.add(new JLabel("Price: "));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Date: "));
        inputPanel.add(dateField);
        inputPanel.add(new JLabel("Amount: "));
        inputPanel.add(amountField);

        JButton addButton = new JButton("Add Record");
        addButton.addActionListener(e -> addRecord());
        inputPanel.add(addButton);

        // Display Section
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Ticker");
        tableModel.addColumn("Price");
        tableModel.addColumn("Date");
        tableModel.addColumn("Amount");
        tableModel.addColumn("Current Price"); // Add this line to include the new column
        tableModel.addColumn("Profit/Loss"); // Add this line to include the new column

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.5);
        frame.getContentPane().add(splitPane);

        JButton deleteButton = new JButton("Delete Record");
        deleteButton.addActionListener(e -> deleteRecord());
        inputPanel.add(deleteButton);

        JButton saveButton = new JButton("Save Portfolio");
        saveButton.addActionListener(e -> savePortfolio());
        frame.getContentPane().add(saveButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void addRecord() {
        String ticker = tickerField.getText().trim();
        String price = priceField.getText().trim();
        String date = dateField.getText().trim();
        String amount = amountField.getText().trim();
    
        if (!isValidTicker(ticker)) {
            JOptionPane.showMessageDialog(frame, "Invalid ticker symbol. It should be up to 4 letters.");
            return;
        }
    
        if (!isValidPrice(price)) {
            JOptionPane.showMessageDialog(frame, "Invalid price. Please enter a valid number.");
            return;
        }
    
        if (!isValidDate(date)) {
            JOptionPane.showMessageDialog(frame, "Invalid date. Please use the format YYYY-MM-DD.");
            return;
        }
    
        if (!isValidAmount(amount)) {
            JOptionPane.showMessageDialog(frame, "Invalid amount. Please enter a valid number.");
            return; 
        }
    
        Vector<String> row = new Vector<>();
        row.add(ticker);
        row.add(price);
        row.add(date);
        row.add(amount);
        
        calculatePriceAndProfitLoss(row);
        
        tableModel.addRow(row); 
    
        // Clear input fields after adding a record
        tickerField.setText("");
        priceField.setText("");
        dateField.setText("");
        amountField.setText("");
    }

    private void calculatePriceAndProfitLoss(Vector<String> row) {
         try {
            double currentPrice = fetchCurrentStockPrice(row.get(0));
            if (currentPrice == -1) {
                JOptionPane.showMessageDialog(frame, "Failed to fetch current price for the stock.");
            }

            double enteredPrice = Double.parseDouble(row.get(1));
            double profitLoss = ((currentPrice - enteredPrice) / enteredPrice) * 100;
            
            row.add(String.valueOf(currentPrice));
            row.add(String.format("%.2f%%", profitLoss)); // Display profit/loss as a percentage
        } catch (URISyntaxException | NumberFormatException e) {
            e.printStackTrace(); // Handle the exception (print or log the error)
        }       
    }
    
    private double fetchCurrentStockPrice(String ticker) throws URISyntaxException {
        String apiKey = "TV3TIQW2A0MEECWB";
        String apiUrl = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + ticker + "&apikey=" + apiKey;
    
        try {
            URI uri = new URI(apiUrl);
            URL url = uri.toURL();
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
    
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject globalQuote = jsonResponse.getJSONObject("Global Quote");
                return Double.parseDouble(globalQuote.getString("05. price"));
            } else {
                System.out.println("HTTP error: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return -1;
    }

    private boolean isValidTicker(String ticker) {
        return ticker.length() <= 4;
    }
    
    private boolean isValidPrice(String price) {
        try {
            double parsedPrice = Double.parseDouble(price);
            // Check if the price is non-negative
            return parsedPrice >= 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }
    
    private boolean isValidDate(String date) {
        try {
            java.time.LocalDate.parse(date);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
    
    private boolean isValidAmount(String amount) {
        try {
            double parsedAmount = Double.parseDouble(amount);
            // Check if the amount is non-negative
            return parsedAmount >= 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    private void deleteRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a record to delete.");
            return;
        }

        int confirmDialog = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirmDialog == JOptionPane.YES_OPTION) {
            tableModel.removeRow(selectedRow);
        }
    }

    private void savePortfolio() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showSaveDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file.getAbsolutePath() + ".csv")) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    // save only the first 4 columns
                    for (int j = 0; j < 4; j++) {
                        writer.write(tableModel.getValueAt(i, j).toString());
                        if (j < tableModel.getColumnCount() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }
                writer.close();
                JOptionPane.showMessageDialog(frame, "Portfolio saved successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving portfolio.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StockPortfolioApp());
    }
}
