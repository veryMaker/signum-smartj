package bt.sample;

import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.Timestamp;
import bt.Transaction;
import bt.ui.EmulatorWindow;

/**
 * An 'odds and evens game' contract that pays double or nothing on a 50% chance.
 * 
 * From the amount sent to the contract the activation fee is subtracted and this
 * resulting amount is doubled if the sender wins.
 * 
 * Every transaction sent to this contract has an even or odd value attributed
 * according to the transaction timestamp. Two blocks in future the winning
 * value (even or odd) is chosen based on the block hash (random source).
 * 
 * A value in the future is used as source for randomness to difficult tampering
 * from malicious miners. For the same reason, a high activation fee is also advisable
 * and a MAX_PAYMENT is set.
 * 
 * @author jjos
 */
public class OddsGame extends Contract {

	Timestamp lastTimestamp;
	Timestamp next;
	Transaction nextTX;
	Address developer;

	static final long MAX_PAYMENT = 2000*ONE_BURST;
	static final String DEV_ADDRESS = "BURST-JJQS-MMA4-GHB4-4ZNZU";

	/**
	 * This method is executed every time a transaction is received by the contract.
	 */
	@Override
	public void txReceived() {
		// Previous block hash is the random value we use
		long blockOdd = getPrevBlockHash();
		Timestamp timeLimit = getPrevBlockTimestamp();
		blockOdd &= 0xffL; // bitwise AND to avoid negative values
		blockOdd %= 2; // MOD 2 to get just 1 or 0

		nextTX = getTxAfterTimestamp(lastTimestamp);

		while (nextTX != null) {
			next = nextTX.getTimestamp();
			if (next.ge(timeLimit))
				break; // only bets before previous block can run now

			lastTimestamp = next;

			long pay = (lastTimestamp.getValue() % 2) - blockOdd;

			if (pay == 0) {
				// pay double (amount already has the activation fee subtracted)
				long amount = nextTX.getAmount();
				amount = amount * 2;
				if(amount > MAX_PAYMENT)
					amount = MAX_PAYMENT;
				sendAmount(amount, nextTX.getSenderAddress());
			}

			nextTX = getTxAfterTimestamp(lastTimestamp);
		}

		if(getCurrentBalance() > MAX_PAYMENT*3)
			sendAmount(MAX_PAYMENT, parseAddress(DEV_ADDRESS));
	}

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creator = Emulator.getInstance().getAddress("CREATOR");
		Address bet1 = Emulator.getInstance().getAddress("BET1");
		Address bet2 = Emulator.getInstance().getAddress("BET2");
		emu.airDrop(creator, 1000 * ONE_BURST);
		emu.airDrop(bet1, 1000 * ONE_BURST);
		emu.airDrop(bet2, 1000 * ONE_BURST);
		Address odds = Emulator.getInstance().getAddress("ODDS");
		emu.createConctract(creator, odds, OddsGame.class.getName(), ONE_BURST);

		emu.forgeBlock();

		// 10 bets each
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);

		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);

		emu.forgeBlock();
		emu.forgeBlock();
		emu.forgeBlock();

		// another transaction to trigger the sorting mechanism
		emu.send(creator, odds, 10*ONE_BURST);
		emu.forgeBlock();

		new EmulatorWindow(OddsGame.class);
	}
}