// Programming Assignment #4 for CSCI3320 - Advanced Programming
// Written by Justin Shapiro


import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.ThreadLocalRandom;

public class DiningPhils extends Thread {
	private static final int MAX_PHIL = 5; // number of Philosophers at the table
	private static int thread_creation_counter = 0; /* if a status update is requested before all threads have been created, 
												     *   only active threads will display on the status table */
	public static Chopstick[] chopsticks = new Chopstick[MAX_PHIL]; 
	public static Philosopher[] philosophers = new Philosopher[MAX_PHIL];
	public static Thread[] thread_array = new Thread[MAX_PHIL];
		
	public static void main(String[] args) {
		int meals_allowed = 5;	// if no arguments are provided, each Philosopher will eat five times
		if (args.length != 0)
			meals_allowed = Integer.parseInt(args[0]);
		
		for (int i = 0; i < MAX_PHIL; i++) {
			chopsticks[i] = new Chopstick();
			
			if (i < 4) 
				philosophers[i] = new Philosopher(i, chopsticks[i], chopsticks[i + 1], meals_allowed);
			else
				philosophers[i] = new Philosopher(i, chopsticks[i], chopsticks[0], meals_allowed);
			
			thread_array[i] = new Thread(philosophers[i], Integer.toString(i));
			thread_creation_counter++;
			
			thread_array[i].start();
		}
	}
	
	public static synchronized void printStatus() {	
		System.out.println("\nPhilosopher    State          Times Eaten");
		System.out.println("-------------------------------------------");
		for (int i = 0; i < thread_creation_counter; i++) {
			System.out.format("%-14s %-14s %-6s %n", thread_array[i].getName(), philosophers[i].getState(), 
			                  philosophers[i].getNumEat());
		}
	}
}

class Philosopher implements Runnable {
	private volatile Chopstick left_chopstick = new Chopstick(), right_chopstick = new Chopstick();
	private int MAX_EAT, num_eat, chair_number;
	private volatile long eating_time, thinking_time;
	enum State { HUNGRY, THINKING, EATING, SLEEPING; }
	private State current_state = State.HUNGRY;
	
	public Philosopher(int chair_num, Chopstick left, Chopstick right, int eat_num) {
		chair_number = chair_num;
		left_chopstick = left;
		right_chopstick = right;
		MAX_EAT = eat_num;
		current_state = State.HUNGRY;
		
		setRandomTimes();
	}
	
	public void run() {
		DiningPhils.printStatus();
		
		while (num_eat < MAX_EAT) {
			changeState(State.HUNGRY);
			
			think();
			
			changeState(State.HUNGRY);	
			try {
				if (pickupChopsticks() == false)
					continue;
			} catch(NullPointerException e) {/* do nothing */}
			
			eat();
		}
		
		changeState(State.SLEEPING);
	}
	
	public void think() { 
		changeState(State.THINKING);
		setRandomTimes();
		
		try {
			Thread.sleep(thinking_time);
		} catch (InterruptedException e) {/* do nothing */}
	}
	
	public boolean pickupChopsticks() {			
		if (left_chopstick.inUse(chair_number) == false)
			left_chopstick.claim(chair_number);
		else
			return false;
		
		if (right_chopstick.inUse(chair_number) == false)
			right_chopstick.claim(chair_number);
		else {
			left_chopstick.release();
			return false;
		}
		
		return true;
	}
	
	public void eat() {
		changeState(State.EATING);
		try {
			Thread.sleep(eating_time);
		} catch (InterruptedException e) {/* do nothing */}
		
		// Release chopsticks after wake
		try {
			left_chopstick.release();
			right_chopstick.release();
		} catch (NullPointerException e) {/* do nothing */}
		
		num_eat++;
	}
	
	public void changeState(State new_state) { 
		if (current_state != new_state) {
			current_state = new_state; 
			DiningPhils.printStatus();
		}
		else
			return;
	}
	
	public String getState() {
		String str_state = "";
		if (current_state == State.HUNGRY)
			str_state = "Hungry";
		else if (current_state == State.THINKING)
			str_state = "Thinking";
		else if (current_state == State.EATING)
			str_state = "Eating";
		else if (current_state == State.SLEEPING)
			str_state = "Sleeping";
		
		return str_state;
	}
	
	public int getNumEat() { return num_eat; }
	
	public void setRandomTimes() {
		eating_time = ThreadLocalRandom.current().nextLong(5000);
		thinking_time = ThreadLocalRandom.current().nextLong(eating_time);
	}
}

class Chopstick {
	static final int NO_OWNER = 5;	
	private volatile int belongs_to = NO_OWNER; /* If belongs_to == 5, no philosopher has reserved this chopstick.
											     * This provides a double-check so that there never will exist a case where
									             *   a philospher rudely steals a chopstick */
										
	private volatile boolean chopstick = false; /* A chopstick with a value of false means that the chopstick is not in use.
											     * When a Chopstick object is created, it will not be in use */
										
	public boolean inUse(int chair_number) { 
		if (belongs_to == NO_OWNER)
			return false;
		else if (chair_number != belongs_to)
			return true;
		else if (chopstick == true)
			return true;
		
		return false;
	}
	
	public void claim(int chair_number) { 
		belongs_to = chair_number;
		chopstick = true; 
	}
	
	public void release() { 
		belongs_to = NO_OWNER;
		chopstick = false; 
	}
}