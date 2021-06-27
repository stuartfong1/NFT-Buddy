package ExampleSetup.sdk;

import com.hedera.hashgraph.sdk.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import io.github.cdimascio.dotenv.Dotenv;
import io.ipfs.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class HederaExamples {
	// Create a new token
	public static boolean CreateToken(String[] tokenData, AccountId myAccountId, PrivateKey myPrivateKey, Client client) throws TimeoutException, PrecheckStatusException, ReceiptStatusException, IOException {
		// https://github.com/ipfs-shipyard/java-ipfs-http-client
        // Store file on IPFS and save the hash
        IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
        NamedStreamable.FileWrapper file;
        try {
        	file = new NamedStreamable.FileWrapper(new File("images/" + tokenData[0]));
        }
        catch (Exception e) {
        	System.out.println("Invalid file name.");
        	return false;
        }
        
        // Catch potential errors in the future
        if(tokenData[2].length() > 100) {
        	System.out.println("Memo too long.");
        	return false;
        }
        try {
        	if(Integer.parseInt(tokenData[3]) < 1) {
        		return false;
        	}
        }
        catch (Exception e) {
        	System.out.println("Invalid price.");
        	return false;
        }
        
        MerkleNode addResult = ipfs.add(file).get(0);
        String hash = addResult.hash.toBase58();
        
      	// Create the token
		TokenCreateTransaction transaction = new TokenCreateTransaction()
		        .setTokenName(tokenData[1])
		        .setTokenMemo(tokenData[2])
		        .setTokenSymbol(hash)
		        .setInitialSupply(1)
		        .setDecimals(0)
		        .setTreasuryAccountId(myAccountId)
		        .setAdminKey(myPrivateKey);
				
		TransactionResponse txResponse = transaction.freezeWith(client)
				.sign(myPrivateKey)
				.execute(client);
		
		TransactionReceipt receipt = txResponse.getReceipt(client);
		
		Status transactionStatus = receipt.status;
		
		System.out.println("Token creation " + transactionStatus);
		
		// Get the token ID from the receipt
		TokenId tokenId = receipt.tokenId;
		System.out.println("The token ID is " + tokenId);
        
		// Output results
		System.out.println("Token Name: " + tokenData[1]);
		System.out.println("Token Description: " + tokenData[2]);
        System.out.println("See the file at: https://ipfs.io/ipfs/" + hash);
        
        // Write ID and price in .csv file
        WriteFile(tokenId.toString(), tokenData[3]);
        
        return true;
	}
	
	// Get Tokens
	public static ArrayList<TokenId> GetTokens(AccountId myAccountId, Client client) throws TimeoutException, PrecheckStatusException {
		AccountBalanceQuery query = new AccountBalanceQuery()
    	    .setAccountId(myAccountId);
    	AccountBalance tokenBalance = query.execute(client);
    	
    	// tokenBalance is a map, we iterate through each token
    	Iterator<TokenId> it = tokenBalance
    			.tokens
    			.keySet()
    			.iterator();

    	// Copy token IDs
    	ArrayList<TokenId> tokenIds = new ArrayList<TokenId>();
    	while(it.hasNext()) {
    		TokenId temp = it.next();
    		if(tokenBalance.tokens.get(temp) > 0) {
    			tokenIds.add(temp);
    		}
    	}
    	
    	return tokenIds;
	}
	
	// Print all tokens
	public static void ListTokens(int maxIndex, ArrayList<TokenId> tokenIds, Client client) throws TimeoutException, PrecheckStatusException {
		// Display token names
    	for(int i = maxIndex - 10; i < maxIndex && i < tokenIds.size(); i++) {
    		TokenInfoQuery tokenQuery = new TokenInfoQuery()
    		    .setTokenId(tokenIds.get(i));
    		String name = tokenQuery.execute(client).name;

    		System.out.println("[" + (i + 1) + "] " + name);
    	}
    	
    	System.out.println("\n[Prev]\t\t\t[Next]");
    	System.out.println("[Quit]");
	}
	
	// Show information about a token
	public static void ShowTokenInfo(TokenId tokenId, Client client) throws TimeoutException, PrecheckStatusException, IOException {
		// Show token name
		TokenInfoQuery tokenQuery = new TokenInfoQuery()
		    .setTokenId(tokenId);
		System.out.println(tokenQuery.execute(client).name);
		
		// Show token ID
		System.out.println("Token ID: " + tokenId.toString());
		
		// Show token file link
		tokenQuery = new TokenInfoQuery()
    		    .setTokenId(tokenId);
		System.out.println("https://ipfs.io/ipfs/" + tokenQuery.execute(client).symbol);
		
		// Show token memo
		tokenQuery = new TokenInfoQuery()
    		    .setTokenId(tokenId);
		System.out.println("\n" + tokenQuery.execute(client).tokenMemo);
	}
	
	// Associate a token with an account
	public static void AssociateToken(TokenId tokenId, AccountId accountId, PrivateKey privateKey, Client client) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
		// Associate a token to an account
		TokenAssociateTransaction transaction = new TokenAssociateTransaction()
		        .setAccountId(accountId)
		        .setTokenIds(Collections.singletonList(tokenId));

		// Freeze the unsigned transaction, sign with the private key of the account that is being associated to a token, submit the transaction to a Hedera network
		TransactionResponse txResponse = transaction
				.freezeWith(client)
				.sign(privateKey)
				.execute(client);

		// Request the receipt of the transaction
		TransactionReceipt receipt = txResponse.getReceipt(client);

		// Get the transaction consensus status
		Status transactionStatus = receipt.status;

		System.out.println("Association " +transactionStatus);
	}

	// Buy a token for Hbars
	public static void BuyToken(AccountId myAccountId, AccountId newAccountId, PrivateKey myPrivateKey, PrivateKey newAccountPrivateKey, int tokenPrice, TokenId tokenId, Client client) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
		// Atomic swap between a Hedera Token Service token and hbar
		TransferTransaction atomicSwap = new TransferTransaction()
			 .addHbarTransfer(newAccountId, Hbar.fromTinybars(-tokenPrice))
			 .addHbarTransfer(myAccountId, Hbar.fromTinybars(tokenPrice))
		     .addTokenTransfer(tokenId, myAccountId, -1)
			 .addTokenTransfer(tokenId, newAccountId, 1)
			 .freezeWith(client);

		// Sign the transaction with accountId1 and accountId2 private keys, submit the transaction to a Hedera network
		TransactionResponse txResponse = atomicSwap
				.sign(myPrivateKey)
				.sign(newAccountPrivateKey)
				.execute(client);

		// Request the receipt of the transaction
		TransactionReceipt receipt = txResponse.getReceipt(client);

		// Get the transaction consensus status
		Status transactionStatus = receipt.status;

		System.out.println("Purchase " + transactionStatus);
	}
	
	// Write token information in a .csv file
	public static void WriteFile(String tokenId, String cost) throws IOException {
		// Initialize file reader
        CSVReader tokenReader = new CSVReader(new FileReader("tokens.csv"));
        
        // Append existing data
        List<String[]> csvData = new ArrayList<String[]>();
        List<String[]> existingData = tokenReader.readAll();
        for(String[] i : existingData) {
        	csvData.add(i);
        }
        
        // Initialize file writer
        CSVWriter tokenWriter = new CSVWriter(new FileWriter("tokens.csv"));
        
        // Add new token
        csvData.add(new String[] {tokenId, cost});
        
        tokenWriter.writeAll(csvData);
        tokenWriter.close();
	}
	
	// Erase the specified token from the file
	public static void EraseFile(String tokenId) throws IOException {
		// Initialize file reader
        CSVReader tokenReader = new CSVReader(new FileReader("tokens.csv"));
        
        // Append existing data
        List<String[]> csvData = new ArrayList<String[]>();
        List<String[]> data = tokenReader.readAll();
        for(String[] i : data) {
        	csvData.add(i);
        }
        
        // Initialize file writer
        CSVWriter tokenWriter = new CSVWriter(new FileWriter("tokens.csv"));
        
        // Search each row for the token ID and remove it when found
        for(int row = 0; row < csvData.size(); row++) {
			if(csvData.get(row)[0].equals(tokenId)) {
				csvData.remove(row);
				break;
			}
		}
        
        // Write the data in the .csv file
        tokenWriter.writeAll(csvData);
        tokenWriter.close();
	}

	// Get the price of a token
	public static int GetPrice(String tokenId) throws IOException {
		// Initialize file reader
        CSVReader tokenReader = new CSVReader(new FileReader("tokens.csv"));
        
        // Get data from file
		List<String[]> data = tokenReader.readAll();
		
		// Search each row for the token ID
		for(String[] row : data) {
			if(row[0].equals(tokenId)) {
				return Integer.parseInt(row[1]);
			}
		}
		
		return 0;
	}
	
	
    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, IOException, ReceiptStatusException {
    	
    	///////////
    	// Setup //
    	///////////
    	
    	Scanner scanner = new Scanner(System.in);
    	
        // Grab your Hedera testnet account ID and private key
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        // Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);

        // Generate a new key pair
        PrivateKey newAccountPrivateKey = PrivateKey.generate();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        // Create new account and assign the public key
        TransactionResponse newAccount = new AccountCreateTransaction()
                .setKey(newAccountPublicKey)
                .setInitialBalance(Hbar.fromTinybars(1000))
                .execute(client);

        // Get the account ID
        AccountId newAccountId = newAccount.getReceipt(client).accountId;
        System.out.println("The account ID is: " + newAccountId);

        // Check the account's balance
        AccountBalance accountBalance = new AccountBalanceQuery()
                .setAccountId(newAccountId)
                .execute(client);
        System.out.println("The account balance is: " + accountBalance.hbars);
        
        ///////////////
        // Start app //
        ///////////////
        
        while (true) {
        	int choice = 0;
            while (true) {
            	
	        	System.out.println("What would you like to do?");
	            System.out.println("[1] Create & sell NFT");
	            System.out.println("[2] View market");
	            System.out.println("[3] My NFTs");
	            System.out.println("[4] Quit");
	            
	            // Get user choice
            	try {
            		choice = Integer.parseInt(scanner.nextLine());
            	}
            	catch (Exception e) {
            		
            	}
            	if(choice > 0 && choice <= 4) {
            		break;
            	}
            	System.out.println("Invalid input.");
            }
            
            
            // Create NFT
            if (choice == 1) {
            	String[] tokenData = new String[4];
            	do {
            		// Input token data
	    	        System.out.println("What is the file you would like to tokenize?");
	    	        tokenData[0] = scanner.nextLine();
	    	        System.out.println("What is the name of your NFT?");
	    	        tokenData[1] = scanner.nextLine();
	    	        System.out.println("Please enter a short description of the NFT (max. 100 chars):");
	    	        tokenData[2] = scanner.nextLine();
	    	        System.out.println("What is the price of your NFT in tℏ?");
	    	        tokenData[3] = scanner.nextLine();
            	}
            	while (!CreateToken(tokenData, myAccountId, myPrivateKey, client));
            }
            
            // Market
            else if (choice == 2) {
            	int marketIndex = 10;
            	while (true) {
            		// Get tokens
	            	ArrayList<TokenId> marketIds = GetTokens(myAccountId, client);
	            	
	            	// Display a list of available tokens
	            	ListTokens(marketIndex, marketIds, client);
	            	
	            	// Get user choice
	            	String marketChoice = scanner.nextLine().toLowerCase();
	            	if (marketChoice.equals("next")) {
	            		marketIndex += 10;
	            	}
	            	else if (marketChoice.equals("prev")) {
	            		marketIndex -= 10;
	            	}
	            	else if (marketChoice.equals("quit")) {
	            		break;
	            	}
	            	else {  // User enters a number
	            		TokenId tokenId;
	            		try {
	            			tokenId = marketIds.get(Integer.parseInt(marketChoice) - 1);
	            		}
	            		catch (Exception e) {
	            			System.out.println("Invalid input.");
	            			continue;
	            		}
	            		
	            		// Display name, id, etc.
	            		ShowTokenInfo(tokenId, client);
	            		
	            		// Show price
	            		int tokenPrice = GetPrice(tokenId.toString());
	            		System.out.println("Price: " + tokenPrice + "tℏ");
	            		
	            		System.out.println("[Buy]\t\t\t[Back]");
	            		String tokenChoice = scanner.nextLine().toLowerCase();
	            		
	            		if(tokenChoice.equals("buy")) {
	            			// Associate token with new owner
	            			AssociateToken(tokenId, newAccountId, newAccountPrivateKey, client);
	            			
	            			// Buy the token for hbars
	            			BuyToken(myAccountId, newAccountId, myPrivateKey, newAccountPrivateKey, tokenPrice, tokenId, client);

	            			// Erase the token from the market
	            			EraseFile(tokenId.toString());
	            		}
	            	}
            	}
            }
            // View bought NFTs
            else if (choice == 3) {
            	int boughtIndex = 10;
            	while (true) {
            		// Get tokens
	            	ArrayList<TokenId> boughtIds = GetTokens(newAccountId, client);
	            	
	            	// Display a list of owned tokens
	            	ListTokens(boughtIndex, boughtIds, client);
	            	
	            	// Get user choice
	            	String boughtChoice = scanner.nextLine().toLowerCase();
	            	if (boughtChoice.equals("next")) {
	            		boughtIndex += 10;
	            	}
	            	else if (boughtChoice.equals("prev")) {
	            		boughtIndex -= 10;
	            	}
	            	else if (boughtChoice.equals("quit")) {
	            		break;
	            	}
	            	else {  // User enters a number
	            		TokenId tokenId;
	            		try {
	            			tokenId = boughtIds.get(Integer.parseInt(boughtChoice) - 1);
	            		}
	            		catch (Exception e) {
	            			System.out.println("Invalid input.");
	            			continue;
	            		}
	            		
	            		// Display name, id, etc.
	            		ShowTokenInfo(tokenId, client);
	            	}
	            }
            }
            // Quit app
            else {
            	break;
            }
        }
        scanner.close();
    }
}
