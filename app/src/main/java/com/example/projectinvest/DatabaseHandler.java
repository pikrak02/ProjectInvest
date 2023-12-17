package com.example.projectinvest;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "InvestmentDatabase";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_STOCKS = "stocks";
    private static final String KEY_ID = "id";
    private static final String KEY_SYMBOL = "symbol";
    private static final String KEY_PURCHASE_DATE = "purchase_date";
    private static final String KEY_SHARES = "shares";

    private static final String KEY_PRICE = "price";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STOCKS_TABLE = "CREATE TABLE " + TABLE_STOCKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_SYMBOL + " TEXT,"
                + KEY_PURCHASE_DATE + " TEXT," + KEY_SHARES + " INTEGER,"
                + KEY_PRICE + " REAL" + ")";
        db.execSQL(CREATE_STOCKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


    }

    public void addStock(String symbol, String purchaseDate, int shares, double price) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SYMBOL, symbol);
        values.put(KEY_PURCHASE_DATE, purchaseDate);
        values.put(KEY_SHARES, shares);
        values.put(KEY_PRICE, price);

        db.insert(TABLE_STOCKS, null, values);
        db.close();
    }

    public void sellStock(String symbol, int sharesToSell, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT " + KEY_ID + ", " + KEY_SHARES + " FROM " + TABLE_STOCKS
                + " WHERE " + KEY_SYMBOL + " = ? ORDER BY " + KEY_PURCHASE_DATE;
        Cursor cursor = db.rawQuery(query, new String[]{symbol});

        int idIndex = cursor.getColumnIndex(KEY_ID);
        int sharesIndex = cursor.getColumnIndex(KEY_SHARES);

        int totalSharesInDatabase = 0;

        while (cursor.moveToNext()) {
            if (idIndex != -1 && sharesIndex != -1) {
                int id = cursor.getInt(idIndex);
                int currentShares = cursor.getInt(sharesIndex);
                totalSharesInDatabase += currentShares;

                if (currentShares <= sharesToSell) {
                    db.delete(TABLE_STOCKS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
                    sharesToSell -= currentShares;
                } else {
                    ContentValues values = new ContentValues();
                    values.put(KEY_SHARES, currentShares - sharesToSell);
                    db.update(TABLE_STOCKS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
                    sharesToSell = 0;
                }
            }
        }

        cursor.close();
        db.close();

        if (sharesToSell > 0) {

            Toast.makeText(context, "Not enough shares of this stock to sell", Toast.LENGTH_SHORT).show();
        }
    }


    public void deleteAllStocks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STOCKS, null, null);
        db.close();
    }

    public List<Stock> getAllStocks() {
        List<Stock> stockList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_STOCKS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);


        int idIndex = cursor.getColumnIndex(KEY_ID);
        int symbolIndex = cursor.getColumnIndex(KEY_SYMBOL);
        int purchaseDateIndex = cursor.getColumnIndex(KEY_PURCHASE_DATE);
        int sharesIndex = cursor.getColumnIndex(KEY_SHARES);
        int priceIndex = cursor.getColumnIndex(KEY_PRICE);

        if (cursor.moveToFirst()) {
            do {

                if (idIndex != -1 && symbolIndex != -1 && purchaseDateIndex != -1 && sharesIndex != -1 && priceIndex != -1) {
                    Stock stock = new Stock();
                    stock.setId(cursor.getInt(idIndex));
                    stock.setSymbol(cursor.getString(symbolIndex));
                    stock.setPurchaseDate(cursor.getString(purchaseDateIndex));
                    stock.setNumberOfShares(cursor.getInt(sharesIndex));
                    stock.setPrice(cursor.getDouble(priceIndex));

                    stockList.add(stock);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return stockList;
    }


}
