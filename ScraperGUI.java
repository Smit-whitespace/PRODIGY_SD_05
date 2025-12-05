package com.example.scraper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

public class ScraperGui {

    private final JFrame frame = new JFrame("Java Scraper GUI - Jsoup Demo");
    private final JTextField urlField = new JTextField("http://books.toscrape.com/");
    private final JButton startBtn = new JButton("Start");
    private final JButton cancelBtn = new JButton("Cancel");
    private final JButton saveBtn = new JButton("Save CSV");
    private final JSpinner maxPagesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JProgressBar progressBar = new JProgressBar();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Title", "Price", "PriceNumeric", "Rating", "Availability", "Product URL", "ScrapedAt"}, 0);
    private final JTable table = new JTable(tableModel);

    private SwingWorker<List<Book>, Void> worker = null;

    public ScraperGui() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        // Top control panel
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridx = 0; c.gridy = 0; c.weightx = 0; top.add(new JLabel("Start URL:"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; top.add(urlField, c);
        c.gridx = 2; c.gridy = 0; c.weightx = 0; top.add(new JLabel("Max pages (0=all):"), c);
        c.gridx = 3; c.gridy = 0; top.add(maxPagesSpinner, c);
        c.gridx = 4; c.gridy = 0; top.add(startBtn, c);
        c.gridx = 5; c.gridy = 0; top.add(cancelBtn, c);
        c.gridx = 6; c.gridy = 0; top.add(saveBtn, c);

        frame.add(top, BorderLayout.NORTH);

        // center table
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // bottom
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(progressBar, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        // initial states
        cancelBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        progressBar.setStringPainted(true);

        // listeners
        startBtn.addActionListener(e -> startScrape());
        cancelBtn.addActionListener(e -> cancelScrape());
        saveBtn.addActionListener(e -> saveCsv());

        frame.setLocationRelativeTo(null);
    }

    private void startScrape() {
        final String url = urlField.getText().trim();
        final int maxPages = (Integer) maxPagesSpinner.getValue();

        // simple validation
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a start URL.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // UI state
        startBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        saveBtn.setEnabled(false);
        tableModel.setRowCount(0);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setString("Starting...");

        Scraper scraper = new Scraper();

        worker = new SwingWorker<>() {
            @Override
            protected List<Book> doInBackground() throws Exception {
                return scraper.scrape(url, maxPages, 800, 1800, itemsScraped -> {
                    // update progress on EDT
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(Math.min(itemsScraped, 100));
                        progressBar.setString(itemsScraped + " items");
                    });
                });
            }

            @Override
            protected void done() {
                try {
                    List<Book> books = get();
                    for (Book b : books) {
                        tableModel.addRow(new Object[]{
                                b.getTitle(), b.getPrice(), b.getPriceNumeric(),
                                b.getRating(), b.getAvailability(), b.getProductUrl(), b.getScrapedAt()
                        });
                    }
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(Math.min(tableModel.getRowCount(), 100));
                    progressBar.setString("Finished: " + tableModel.getRowCount() + " items");
                    saveBtn.setEnabled(tableModel.getRowCount() > 0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    progressBar.setString("Error");
                } finally {
                    startBtn.setEnabled(true);
                    cancelBtn.setEnabled(false);
                    worker = null;
                }
            }
        };

        worker.execute();
    }

    private void cancelScrape() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
            progressBar.setString("Cancelled");
            startBtn.setEnabled(true);
            cancelBtn.setEnabled(false);
        }
    }

    private void saveCsv() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame, "No data to save.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(Path.of("scraped_books.csv").toFile());
        int ret = chooser.showSaveDialog(frame);
        if (ret != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter fw = new FileWriter(chooser.getSelectedFile());
             CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT.withHeader(
                     "title","price","price_numeric","rating","availability","product_url","scraped_at"))) {

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                printer.printRecord(
                        tableModel.getValueAt(i, 0),
                        tableModel.getValueAt(i, 1),
                        tableModel.getValueAt(i, 2),
                        tableModel.getValueAt(i, 3),
                        tableModel.getValueAt(i, 4),
                        tableModel.getValueAt(i, 5),
                        tableModel.getValueAt(i, 6)
                );
            }
            printer.flush();
            JOptionPane.showMessageDialog(frame, "CSV saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public static void main(String[] args) {
        ScraperGui g = new ScraperGui();
        g.show();
    }
}
