# NFT Buddy
Grade 12 culminating project for ICS4UA written in Java in Eclipse. It is a program that allows the user to tokenize a file using Hedera. It also includes a market, where it simulates the exchange of non-fungible tokens for hbars.

## Setup
### Create a Hedera Testnet Account
Create an account here: https://portal.hedera.com/register

Type the account ID, public key, and private key for the testnet in the program's .env file.

### Download IPFS
Download IPFS here: https://docs.ipfs.io/install/command-line/#system-requirements

Once downloaded, navigate to the folder in your terminal, and run the command "ipfs daemon".

### Run the program
Open the project in Eclipse, and click on the green "run" button on the top of the screen. After that the program should be up and running!

![Main page](https://i.imgur.com/Bu2Oaz1.png)

## Token Creation
To allow the program to access the file, place it inside the images source folder. There are a few example files already in the folder. Then, follow the program instructions to create the NFT on the hashgraph and receive a link to the file on IPFS (https://ipfs.io/).

![Image creation](https://i.imgur.com/unkHbai.png)

## Market
View a collection of created NFTs here. Type "prev" or "next" to go to another page, or type the number of an NFT to view the information about it. Buying an NFT is as simple as typing "buy", and the token will be transferred onto the temporary customer account.

![Market](https://i.imgur.com/D7xf5b1.png)

## My NFTs
The NFTs bought by the temporary account are displayed here. Type the number of the NFT to view its information. Once the program is closed, these tokens will disappear.

![My NFTs](https://i.imgur.com/vZ6tD58.png)

#### Disclaimer: I do not own any of the files in the images folder.
