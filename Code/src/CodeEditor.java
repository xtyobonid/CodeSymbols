import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

public class CodeEditor extends JPanel implements KeyListener{
	
	private static final int CONSONANT_HEIGHT = 150;
	private static final int CONSONANT_WIDTH = 100;
	private static final int VOWEL_HEIGHT = 50;
	private static final int VOWEL_WIDTH = 50;
	private static final int TOTAL_HEIGHT = VOWEL_HEIGHT + CONSONANT_HEIGHT + VOWEL_HEIGHT;
	private static final int VERTICAL_ADJ = 50;
	private static final int VOWEL_CENTERING_ADJ = 25;
	private static final int SCREEN_WIDTH = 1500;
	private static final int SCREEN_HEIGHT = 800;
	private static final int CPL = (SCREEN_WIDTH / CONSONANT_WIDTH) - 1; //characters per line
	
	private static final long serialVersionUID = 8116928570263072271L; //to get rid of eclipse warning
	private ArrayList<ArrayList<String>> groups;
	private ArrayList<String> toDraw;
	//private Map<String, File> consonants; //idk how to draw image, maybe not buffered
	//private Map<String, File> vowels;
	private Map<Character, Integer> letterCounts;
	private String text;
	
	public CodeEditor() {
		setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
		setVisible(true);
        setBackground(Color.white);
        
        text = "";
        groups = new ArrayList<ArrayList<String>>();
        toDraw = new ArrayList<String>();
        //consonants = new HashMap<String, File>();
        //vowels = new HashMap<String, File>();
        letterCounts = new HashMap<Character, Integer>();
		 
		//new Timer(17, (ActionEvent e) -> {
            //repaint();
        //}).start();
	}
	
	//numConsonants: 37, numVowels: 11, double vowels and space consonants will be loaded at runtime, bc 11!/11^2 double vowels and 148 space consonants
	public void preloadImages() {
		//pre-load consonants
		File dir = new File("consonants");
		File[] directoryListing = dir.listFiles();
		for (File child : directoryListing) {
			//System.out.println("" + child.getName());
			//consonants.put(child.getName(), child);
		}
		//pre-load vowels
		dir = new File("vowels");
		directoryListing = dir.listFiles();
		for (File child : directoryListing) {
			//System.out.println("" + child.getName());
			//vowels.put(child.getName(), child);
		}
	}
	
	public void updateGrouping() {
		groups = new ArrayList<ArrayList<String>>();
		String[] temp = text.split(" ");
		for (int i = 0; i < temp.length; i++) { // loop for each word
			groups.add(new ArrayList<String>());
			for (int j = 0; j < temp[i].length(); j++) { //loop for each char in word temp[i]
				if (j % 2 == 0) {
					groups.get(i).add(new String("")); //once size gets to 2, new string in array
				}
				groups.get(i).set(groups.get(i).size() - 1, groups.get(i).get(groups.get(i).size() - 1) + temp[i].substring(j, j + 1)); //update last string to include char at temp[i].charAt(j);
			}
		}
		//groups now has each word split into groups of length 2
		fixConsonantGroupings();
		fixVowelGroupings();
		prepareGroupings(); //get groups ready for drawing
		repaint();
	}
	
	//moves all groups to one array (toDraw), adds spaces to end of groups 
	public void prepareGroupings() {
		toDraw.clear();
		for (int i = 0; i < groups.size(); i++) {
			for (int j = 0; j < groups.get(i).size(); j++) {
				if (j == groups.get(i).size() - 1) {
					toDraw.add(groups.get(i).get(j) + " ");
				} else {
					toDraw.add(groups.get(i).get(j));
				}
			}
		}
	}
	
	//first step after splitting into groups of 2
	//splits up any groups of two that have two consonants
	public void fixConsonantGroupings() { //queueing -> qu eu ei ng -> qu eu ei n g
		for (int i = 0; i < groups.size(); i++) {
			for (int j = 0; j < groups.get(i).size(); j++) {
				String group = groups.get(i).get(j);
				//System.out.println("test: " + group);
				if (group.length() >= 2 && isConsonant(group.charAt(0)) && isConsonant(group.charAt(1))) { // if it is, split them
					groups.get(i).set(j,     "" + group.charAt(0));
					groups.get(i).add(j + 1, "" + group.charAt(1));
				}
			}
		}
	}
	
	public void fixVowelGroupings() { //queueing -> qu eu ei ng -> qu eu ei n g -> que uei n g -> que ue in g -> que u ein g          hello -> he ll o -> he l l o -> he l lo
		if (!checkGroupValidity()) { // break if-statement, if valid, it breaks the recursive "loop"
			for (int i = 0; i < groups.size(); i++) {
				for (int j = 0; j < groups.get(i).size(); j++) {
					String group = groups.get(i).get(j);
					if (!containsConsonant(group)) { //if there's no consonant then 
						if (group.length() > 1) { //attempt split left and split right, bc group is longer than a char
							if (j > 0 && canTakeRightVowel(groups.get(i).get(j - 1))) { //split left
								char splitLeft = group.charAt(0); //first char in this group
								groups.get(i).set(j - 1, groups.get(i).get(j - 1) + splitLeft); //append char
								groups.get(i).set(j, group.substring(1)); //update this group to omit first char
							}
							group = groups.get(i).get(j); //update to prepare for removal of this group if split right
							if (j < groups.get(i).size() - 1 && canTakeLeftVowel(groups.get(i).get(j + 1))) { // split right
								char splitRight = group.charAt(group.length() - 1); // last char in this group
								groups.get(i).set(j + 1, splitRight + groups.get(i).get(j + 1)); //prepend char
								if (group.length() == 1) { //its checking length 1 b/c it hasn't updated since before this split right
									groups.get(i).remove(j);
								} else {
									groups.get(i).set(j, group.substring(0, group.length() - 1)); //update this group to omit last char
								}
							} 
						} else { //attempt split left or split right, not both, b/c only 1 char
							if (j > 0 && canTakeRightVowel(groups.get(i).get(j - 1))) { //split left
								char splitLeft = group.charAt(0); //only char in this group
								groups.get(i).set(j - 1, groups.get(i).get(j - 1) + splitLeft); //append char
								groups.get(i).remove(j);
							} else if (j < groups.get(i).size() - 1 && canTakeLeftVowel(groups.get(i).get(j + 1))) { // split right
								char splitRight = group.charAt(0); // only char in this group
								groups.get(i).set(j + 1, splitRight + groups.get(i).get(j + 1)); //prepend char
								groups.get(i).remove(j);
							} else { // without this, single letters like "i" wont work, as well as words that start with vowels
								return;
							}
						}
					}
				}
			}
			System.out.println(groups);
			fixVowelGroupings(); //recursion! 
		}
	}
	
	public boolean checkGroupValidity() {
		for (int i = 0; i < groups.size(); i++) { //[[h]]
			for (int j = 0; j < groups.get(i).size(); j++) {
				String temp = groups.get(i).get(j);
				if (temp.length() > 5) {
					return false;
				}
				int consonantCount = 0;
				int consonantIndex = -1;
				for (int k = 0; k < temp.length(); k++) {
					if (isConsonant(temp.charAt(k))) {
						consonantCount++;
						consonantIndex = k;
					}
				}
				if (consonantCount != 1) {
					return false;
				}
				if (consonantIndex > 2 || temp.length() - consonantIndex - 1 > 2) {
					return false;
				}
			}
		}
		return true;
	}
 
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        //Graphics2D g = (Graphics2D) gg;
       // try {
			//g.drawImage(ImageIO.read(consonants.get("b1")), 200, 200, this);
		//} catch (IOException e1) {e1.printStackTrace();}
        //System.out.println(consonants.size());
        //File test = new File("consonants/b2.png");
        //try {
		//	g.drawImage(ImageIO.read(test), 200, 200, this);
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
        //g.draw3DRect(15, 15, 15, 15, true);
        //System.out.println(vowels);
        g.setColor(Color.BLACK);
       // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                //RenderingHints.VALUE_ANTIALIAS_ON);
        int counter = 1;
        int horizontalCharCounter = 0;
        int lineCounter = 0;
        if (!toDraw.isEmpty()) {
        	for (int i = 0; i < toDraw.size(); i++) {
        		String group = toDraw.get(i);
        		int consonantIndex = findConsonant(group);
        		if (consonantIndex != -1) {
	        		char consonant = group.charAt(consonantIndex);
	        		String preVowels = group.substring(0, group.indexOf(consonant));
	        		String postVowels;
	        		if (group.indexOf(" ") != -1) {
	        			postVowels = group.substring(group.indexOf(consonant) + 1, group.indexOf(" "));
	        		} else {
	        			postVowels = group.substring(group.indexOf(consonant) + 1);
	        		}
	        		//check if preVowels or postVowels are double vowels
	        			//if so load image from doublevowels folder
	        		//check if group contains space
	        			//if so load space consonant from spaceconsonant folder rather than drawing normal consonant
	        		//draw preVowels above
	        		for (int j = 0; j < preVowels.length(); j++) {
	        			String key = "" + preVowels.charAt(j);
	        			key += getLetterRemainder(counter, letterCounts.get(preVowels.charAt(j)));
	        			//System.out.println("pkey: " + key);
	        			if (preVowels.length() > 1) {
	        				try {
								g.drawImage(ImageIO.read(new File("vowels/"+ key + ".png")), (CONSONANT_WIDTH * horizontalCharCounter) + (j * VOWEL_WIDTH), VERTICAL_ADJ + (TOTAL_HEIGHT * lineCounter), this);
							} catch (IOException e) {e.printStackTrace();}
	        			} else {
	        				try {
								g.drawImage(ImageIO.read(new File("vowels/"+ key + ".png")), (CONSONANT_WIDTH * horizontalCharCounter) + VOWEL_CENTERING_ADJ, VERTICAL_ADJ + (TOTAL_HEIGHT * lineCounter), this);
							} catch (IOException e) {e.printStackTrace();}
	        			}
	        			counter++;
	        		}
	        			//increment counter between each
	        			//remember if double, keep counter for 1, increment after first for the second
	        		//if space consonant
	        			//draw space consonant
	        			//increment counter by 1 after drawing
	        		//else (aka normal consonant)
	        		String ckey = "" + consonant;
	        		ckey += getLetterRemainder(counter, letterCounts.get(consonant));
        			//System.out.println("ckey: " + ckey + " counter: " + counter + " lettercount: " + letterCounts.get(consonant));
        			//System.out.println(letterCounts);
        			//File test1 = new File("consonants/b1.png");
        			//System.out.println(test1.getAbsolutePath());
        			if (group.indexOf(" ") != -1) {
        				try {
							g.drawImage(ImageIO.read(new File("spaceconsonants/" + ckey + "s" + getLetterRemainder(counter + postVowels.length() + 1,  4) + ".png")), (CONSONANT_WIDTH * horizontalCharCounter), VERTICAL_ADJ + VOWEL_HEIGHT + (TOTAL_HEIGHT * lineCounter), this);
						} catch (IOException e) {e.printStackTrace();}
        			} else {
	        			try {
							g.drawImage(ImageIO.read(new File("consonants/" + ckey + ".png")), (CONSONANT_WIDTH * horizontalCharCounter), VERTICAL_ADJ + VOWEL_HEIGHT + (TOTAL_HEIGHT * lineCounter), this);
						} catch (IOException e) {e.printStackTrace();}
        			}
        			counter++;
	        			//draw normal consonant
	        			//increment counter by 1 after drawing
	        		//draw postVowels below
        			for (int j = 0; j < postVowels.length(); j++) {
	        			String key = "" + postVowels.charAt(j);
	        			key += getLetterRemainder(counter, letterCounts.get(postVowels.charAt(j)));
	        			//System.out.println("keyp: " + key);
	        			if (postVowels.length() > 1) {
	        				try {
								g.drawImage(ImageIO.read(new File("vowels/"+ key + ".png")), (CONSONANT_WIDTH * horizontalCharCounter) + (j * VOWEL_WIDTH), VERTICAL_ADJ + VOWEL_HEIGHT + CONSONANT_HEIGHT + (TOTAL_HEIGHT * lineCounter), this);
							} catch (IOException e) {e.printStackTrace();}
	        			} else {
	        				try {
								g.drawImage(ImageIO.read(new File("vowels/"+ key + ".png")), (CONSONANT_WIDTH * horizontalCharCounter) + VOWEL_CENTERING_ADJ, VERTICAL_ADJ + VOWEL_HEIGHT + CONSONANT_HEIGHT + (TOTAL_HEIGHT * lineCounter), this);
							} catch (IOException e) {e.printStackTrace();}
	        			}
	        			counter++;
	        		}
	        			//increment counter between each
	    				//remember if double, keep counter for 1, increment after first for the second
	        		//if consonant was space consonant
        			if (group.indexOf(" ") != -1) {
        				counter++;
        			}
	        			//increment counter by 1, then go to next group
        			//increment horizontalCharCounter by 1 after drawing
        			horizontalCharCounter++;
        			if (horizontalCharCounter > CPL) {
        				horizontalCharCounter = 0;
        				lineCounter++;
        			}
        			//System.out.println("test: prevowels: " + preVowels + " consonants: " + consonant + " postvowels: " + postVowels);
        		}
        	}
        }
    }
    
    public static int getLetterRemainder(int counter, int lettVariants) {
    	int temp = counter;
    	while (temp > lettVariants) {
    		temp -= lettVariants;
    	}
    	return temp;
    }
    
    public static void main(String[] args) throws IOException {
    	SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setTitle("Secret Code");
            f.setResizable(false);
            CodeEditor coder = new CodeEditor();
            try {
				coder.loadLetterVariantCounts();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//coder.preloadImages();
            f.add(coder, BorderLayout.CENTER);
    		f.addKeyListener(coder);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

	@Override
	public void keyPressed(KeyEvent e) {
		String key = KeyEvent.getKeyText(e.getKeyCode());
		
		key = key.toLowerCase();
		if (key.matches("[a-z]")) {
			text += key;
			updateGrouping();
		} else if (key.equals("space")) { //"Space"
			text += " ";
			updateGrouping();
		} else if (key.equals("backspace")) { //"Backspace"
			if (text.length() > 0) {
				text = text.substring(0, text.length() - 1);
			}
			updateGrouping();
		}
		System.out.println("Raw Text: " + text);
		System.out.println("Grouping: " + groups);
	}
	
	//checks if the string "group" can handle another vowel from the left
	public static boolean canTakeLeftVowel(String group) { 
		int consonantIndex = findConsonant(group);
		return (consonantIndex <= 1 || consonantIndex == -1);
	}
	
	//checks if the string "group" can handle another vowel from the left
	public static boolean canTakeRightVowel(String group) { 
		int consonantIndex = findConsonant(group); // p e e
		//if (group.length() - consonantIndex - 1 <= 1 || consonantIndex == -1) {
		//	System.out.println("it can take a right vowel, group: " + group);
		//}
		return (group.length() - consonantIndex - 1 <= 1 || consonantIndex == -1);
	}
	
	public void loadLetterVariantCounts() throws IOException {
		File file = new File("lettervariantcounts.txt");
		Scanner in = new Scanner(file);
		while (in.hasNextLine()) {
			String line = in.nextLine();
			char letter = line.charAt(0);
			int count = Integer.parseInt("" + line.charAt(2));
			letterCounts.put(letter, count);
		}
		in.close();
		//System.out.println(letterCounts);
	}
	
	public static int findConsonant(String s) {
		int consonantIndex = -1;
		for (int i = 0; i < s.length(); i++) { //find the consonant
			if (isConsonant(s.charAt(i))) {
				consonantIndex = i;
				break;
			}
		}
		return consonantIndex;
	}
	
	public static boolean isConsonant(char c) {
	    String cons = "bcdfghjklmnpqrstvwxyz";
	    return (cons.indexOf(c) != -1);
	}
	
	public static boolean containsConsonant(String group) { 
		for (int i = 0; i < group.length(); i++) {
			if (isConsonant(group.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//Not necessary
	}

	@Override
	public void keyTyped(KeyEvent e) {
		//Not necessary
	}
}
