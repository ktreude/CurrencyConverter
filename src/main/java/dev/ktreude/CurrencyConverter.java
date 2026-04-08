package dev.ktreude;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * A command-line utility for converting currency values in real-time.
 * This class uses the ExchangeRate-API to fetch the latest conversion rates
 * and provides formatted output based on the target currency's locale and symbol.
 *
 * @author ktreude
 * @version 1.0
 */

public class CurrencyConverter {

    /**
     * The method performs the following steps:
     * Collects source currency, target currency, and amount from the user.
     * Requests conversion data from an external REST API.
     * Parses the JSON response to extract the specific conversion rate.
     * Calculates the converted total and formats it with the correct currency symbol.
     * @param args Command-line arguments (not utilized).
     * @throws IOException If a network or stream error occurs during the API call.
     */

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("Type currency to convert from (e.g., USD):");
            String convertFrom = scanner.nextLine().toUpperCase();

            System.out.println("Type currency to convert to (e.g., EUR):");
            String convertTo = scanner.nextLine().toUpperCase();

            System.out.println("Type quantity to convert:");
            BigDecimal quantity = scanner.nextBigDecimal();

            String apiKey = System.getenv("EXCHANGE_RATE_API_KEY");

            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("Error: API Key not found in environment variables.");
                return;
            }

            String urlString = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + convertFrom;

            // Build the network request
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlString)
                    .get()
                    .build();

            // Execute the network request
            Response response = client.newCall(request).execute();

            // Check for HTTP errors
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    // The API returns 404 Not Found when the base currency doesn't exist
                    System.out.println("Input Error: The base currency '" + convertFrom + "' is not supported or does not exist.");
                } else {
                    // Catches other issues like 500 Internal Server Error
                    System.out.println("Network Error: API request failed with HTTP code " + response.code());
                }
                return;
            }

            String stringResponse = response.body().string();
            JSONObject jsonObject = new JSONObject(stringResponse);

            // Verify the API itself returned a "success" result
            if (jsonObject.has("result") && !jsonObject.getString("result").equals("success")) {
                System.out.println("API Error: " + jsonObject.optString("error-type", "Unknown API error"));
                return;
            }

            JSONObject ratesObject = jsonObject.getJSONObject("conversion_rates");

            // Check if the target currency exists in the API response
            if (!ratesObject.has(convertTo)) {
                System.out.println("Error: Target currency '" + convertTo + "' is not supported by the API.");
                return;
            }

            BigDecimal rate = ratesObject.getBigDecimal(convertTo);
            BigDecimal result = rate.multiply(quantity);

            // Format the output
            Currency targetCurrency = Currency.getInstance(convertTo);
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
            currencyFormatter.setCurrency(targetCurrency);

            System.out.println("Converted amount: " + currencyFormatter.format(result));

        } catch (InputMismatchException e) {
            // Triggers if the user types letters instead of a number for the quantity
            System.out.println("Input Error: Please enter a valid number for the quantity.");
        } catch (IOException e) {
            // Triggers if there is no internet connection or the host is unreachable
            System.out.println("Connection Error: Could not reach the exchange rate API. Please check your internet connection.");
        } catch (JSONException e) {
            // Triggers if the API response isn't formatted the correct way
            System.out.println("Data Error: Received unexpected or malformed data from the API.");
        } catch (Exception e) {
            // Safety net for anything else that might go wrong
            System.out.println("An unexpected error occurred: " + e.getMessage());
        } finally {
            // Ensures the scanner is closed to prevent resource leaks, even if an error occurs
            scanner.close();
        }
    }
}
