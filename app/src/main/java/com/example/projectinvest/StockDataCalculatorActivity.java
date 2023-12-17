package com.example.projectinvest;

import androidx.appcompat.app.AppCompatActivity;
import com.example.projectinvest.api.TwelveDataApiClient;

import android.app.DatePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;

import android.net.ParseException;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ArrayAdapter;



public class StockDataCalculatorActivity extends AppCompatActivity {

    private AutoCompleteTextView stockSymbol;
    private TextView stockDataResult;
    private Button fetchDataButton;

    private EditText startDate, endDate, monthlyContribution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_data_calculator);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        monthlyContribution = findViewById(R.id.monthlyContribution);
        stockDataResult = findViewById(R.id.stockDataResult);
        fetchDataButton = findViewById(R.id.fetchDataButton);

        stockSymbol = findViewById(R.id.stockSymbol);
        stockSymbol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 1) {
                    loadStockSymbols(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(startDate);
            }
        });

        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(endDate);
            }
        });

        fetchDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchStockData();
            }
        });
    }

    private void fetchStockData() {
        String symbol = stockSymbol.getText().toString();
        String start = startDate.getText().toString();
        String end = endDate.getText().toString();
        String monthlyInvestmentString = monthlyContribution.getText().toString();

        if (symbol.isEmpty() || start.isEmpty() || end.isEmpty() || monthlyInvestmentString.isEmpty()) {
            displayErrorMessage("Please fill in all fields.");
            return;
        }

        double monthlyInvestment;
        try {
            monthlyInvestment = Double.parseDouble(monthlyInvestmentString);
        } catch (NumberFormatException e) {
            displayErrorMessage("Invalid monthly contribution amount.");
            return;
        }

        if (isDateInFuture(start) || isDateInFuture(end)) {
            displayErrorMessage("Dates cannot be in the future.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = fetchDataInChunks(symbol, start, end);
                    Log.d("API_RESPONSE", response);
                    calculateInvestmentReturns(response);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> displayErrorMessage("Error fetching data."));
                }
            }
        }).start();
    }

    private boolean isDateInFuture(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date date = sdf.parse(dateStr);
            return date != null && date.after(new Date());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void displayErrorMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stockDataResult.setText(message);
            }
        });
    }


    private String fetchDataInChunks(String symbol, String start, String end) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        try {
            startCal.setTime(sdf.parse(start));
            endCal.setTime(sdf.parse(end));
        } catch (ParseException | java.text.ParseException e) {
            e.printStackTrace();
            return "";
        }

        StringBuilder Result = new StringBuilder();
        while (startCal.get(Calendar.YEAR) + 19 < endCal.get(Calendar.YEAR)) {
            Calendar tempEndCal = (Calendar) startCal.clone();
            tempEndCal.add(Calendar.YEAR, 19);
            String tempResponse = new TwelveDataApiClient().getTimeSeriesData(symbol, "1day", sdf.format(startCal.getTime()), sdf.format(tempEndCal.getTime()));
            Result.append(tempResponse);
            startCal.add(Calendar.YEAR, 19);
        }

        String finalResponse = new TwelveDataApiClient().getTimeSeriesData(symbol, "1day", sdf.format(startCal.getTime()), end);
        Result.append(finalResponse);

        return Result.toString();
    }


    private void calculateInvestmentReturns(String apiResponse) {
        double monthlyInvestment = Double.parseDouble(monthlyContribution.getText().toString());
        final double[] totalInvestment = {0};
        double totalSharesPurchased = 0;
        double finalValue;
        StringBuilder monthlyBreakdown = new StringBuilder();

        try {
            JSONObject jsonResponse = new JSONObject(apiResponse);
            JSONArray values = jsonResponse.getJSONArray("values");

            String lastProcessedMonth = "";
            double totalValue = 0;
            for (int i = values.length() - 1; i >= 0; i--) {
                JSONObject dayValue = values.getJSONObject(i);
                String datetime = dayValue.getString("datetime");
                double closePrice = dayValue.getDouble("close");

                String currentMonth = datetime.substring(0, 7);
                if (!currentMonth.equals(lastProcessedMonth)) {
                    totalSharesPurchased += (monthlyInvestment / closePrice);
                    totalValue = totalSharesPurchased * closePrice;
                    monthlyBreakdown.append(String.format(Locale.US, "End of %s: $%,.2f\n", currentMonth, totalValue));
                    lastProcessedMonth = currentMonth;

                    totalInvestment[0] += monthlyInvestment;
                }
            }

            double lastClosePrice = values.getJSONObject(0).getDouble("close");
            finalValue = totalSharesPurchased * lastClosePrice;

            final double totalInterest = finalValue - totalInvestment[0];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stockDataResult.setText(String.format(Locale.US, "Final Investment Value: $%,.2f\nTotal Invested: $%,.2f\nTotal Interest: $%,.2f", finalValue, totalInvestment[0], totalInterest));
                    TextView monthlyBreakdownView = findViewById(R.id.monthlyBreakdownView);
                    monthlyBreakdownView.setText(monthlyBreakdown.toString());
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadStockSymbols(String query) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = callSymbolSearchApi(query);
                    List<String> symbols = parseSymbolsFromResponse(response);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(StockDataCalculatorActivity.this, android.R.layout.simple_dropdown_item_1line, symbols);
                            stockSymbol.setAdapter(adapter);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }

    private String callSymbolSearchApi(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://twelve-data1.p.rapidapi.com/symbol_search?symbol=" + query + "&outputsize=30")
                .get()
                .addHeader("X-RapidAPI-Key", "683a5fe792msh21d9cb793fbc1c2p15eaf8jsnf0bfe49f664b")
                .addHeader("X-RapidAPI-Host", "twelve-data1.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private List<String> parseSymbolsFromResponse(String response) {
        List<String> symbols = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray dataArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject symbolObject = dataArray.getJSONObject(i);
                String symbol = symbolObject.getString("symbol");
                symbols.add(symbol);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return symbols;
    }





    private void showDatePickerDialog(final EditText dateField) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel(dateField, calendar);
            }
        };

        new DatePickerDialog(StockDataCalculatorActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateLabel(EditText editText, Calendar calendar) {
        String format = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }
}
