package com.example.projectinvest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectinvest.api.TwelveDataApiClient;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DemoInvestmentActivity extends AppCompatActivity {

    private EditText etStockSymbol, etPurchaseDate, etNumberOfShares;
    private Button btnBuyStock, btnSellStock;
    private TextView tvPortfolio;
    private DatabaseHandler dbHandler;
    private Button btnOrder, btnBuyMode, btnSellMode;
    private View layoutPurchaseDate;

    private TwelveDataApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_investment);


        etStockSymbol = findViewById(R.id.etStockSymbol);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etNumberOfShares = findViewById(R.id.etNumberOfShares);
        btnBuyStock = findViewById(R.id.btnBuyMode);
        btnSellStock = findViewById(R.id.btnSellMode);
        tvPortfolio = findViewById(R.id.tvPortfolio);
        dbHandler = new DatabaseHandler(this);
        apiClient = new TwelveDataApiClient();

        etPurchaseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        btnBuyStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buyStock();
            }
        });

        btnSellStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sellStock();
            }
        });

        updatePortfolioView();

        btnBuyMode = findViewById(R.id.btnBuyMode);
        btnSellMode = findViewById(R.id.btnSellMode);
        btnOrder = findViewById(R.id.btnOrder);
        layoutPurchaseDate = findViewById(R.id.layoutPurchaseDate);

        btnBuyMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBuyMode();
            }
        });

        btnSellMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSellMode();
            }
        });

        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutPurchaseDate.getVisibility() == View.VISIBLE) {
                    buyStock();
                } else {
                    sellStock();
                }
            }
        });

        setBuyMode();
    }

    private void setBuyMode() {
        layoutPurchaseDate.setVisibility(View.VISIBLE);
        btnOrder.setText("Buy");
    }

    private void setSellMode() {
        layoutPurchaseDate.setVisibility(View.GONE);
        btnOrder.setText("Sell");
    }

    private void buyStock() {
        String stockSymbol = etStockSymbol.getText().toString();
        String purchaseDate = etPurchaseDate.getText().toString();
        String numberOfSharesStr = etNumberOfShares.getText().toString();

        if (stockSymbol.isEmpty() || purchaseDate.isEmpty() || numberOfSharesStr.isEmpty()) {

            return;
        }

        int numberOfShares = Integer.parseInt(numberOfSharesStr);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double currentPrice = apiClient.getCurrentStockPrice(stockSymbol);
                    dbHandler.addStock(stockSymbol, purchaseDate, numberOfShares, currentPrice);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updatePortfolioView();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }


    private void sellStock() {
        String stockSymbol = etStockSymbol.getText().toString();
        String numberOfSharesStr = etNumberOfShares.getText().toString();

        if (stockSymbol.isEmpty() || numberOfSharesStr.isEmpty()) {

            return;
        }

        int numberOfShares = Integer.parseInt(numberOfSharesStr);

        dbHandler.sellStock(stockSymbol, numberOfShares, this);
        updatePortfolioView();

    }


    private void updatePortfolioView() {
        List<Stock> portfolio = dbHandler.getAllStocks();
        double totalPortfolioValue = 0;
        StringBuilder portfolioDisplay = new StringBuilder();

        for (Stock stock : portfolio) {
            double stockValue = stock.getNumberOfShares() * stock.getPrice();
            totalPortfolioValue += stockValue;
        }

        for (Stock stock : portfolio) {
            double stockValue = stock.getNumberOfShares() * stock.getPrice();
            double percentageOfPortfolio = (stockValue / totalPortfolioValue) * 100;
            portfolioDisplay.append(String.format(Locale.US, "Symbol: %s, Shares: %d, Value: $%.2f, Portfolio %%: %.2f%%\n",
                    stock.getSymbol(), stock.getNumberOfShares(), stockValue, percentageOfPortfolio));
        }

        tvPortfolio.setText(portfolioDisplay.toString());
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                        etPurchaseDate.setText(selectedDate);
                    }
                }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

}
