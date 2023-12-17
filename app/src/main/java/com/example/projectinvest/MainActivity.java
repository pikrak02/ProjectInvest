package com.example.projectinvest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private EditText initialDeposit, interestRate, monthlyAddition, investmentDuration;
    private TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initialDeposit = findViewById(R.id.initialDeposit);
        interestRate = findViewById(R.id.interestRate);
        monthlyAddition = findViewById(R.id.monthlyAddition);
        investmentDuration = findViewById(R.id.investmentDuration);
        Button calculateButton = findViewById(R.id.calculateButton);
        resultView = findViewById(R.id.resultView);


        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateCompoundInterest();
            }
        });
    }

    private void calculateCompoundInterest() {
        try {
            double firstDeposit = Double.parseDouble(initialDeposit.getText().toString());
            double annualRate = Double.parseDouble(interestRate.getText().toString());
            double monthlyRate = annualRate / 12 / 100;
            double monthlyContribution = Double.parseDouble(monthlyAddition.getText().toString());
            int years = Integer.parseInt(investmentDuration.getText().toString());

            double futureValue = firstDeposit;
            StringBuilder breakdown = new StringBuilder();
            breakdown.append("Yearly Breakdown:\n\n");
            for (int i = 1; i <= years; i++) {
                for (int j = 0; j < 12; j++) {
                    futureValue += monthlyContribution;
                    futureValue += futureValue * monthlyRate;
                }
                breakdown.append(String.format(Locale.US, "Year %2d: $%,.2f\n", i, futureValue));
            }

            resultView.setText(String.format("Final Amount: $%,.2f", futureValue));
            TextView detailBreakdownView = findViewById(R.id.detailBreakdownView);
            detailBreakdownView.setText(breakdown.toString());
        } catch (NumberFormatException e) {
            resultView.setText("Please enter valid numbers");
            TextView detailBreakdownView = findViewById(R.id.detailBreakdownView);
            detailBreakdownView.setText("");
        }
    }


}
