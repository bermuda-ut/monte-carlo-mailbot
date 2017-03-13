package automail;

import strategies.*;

public class Automail {
	      
    public Robot robot;
    public IMailPool mailPool;
    
    Automail(IMailDelivery delivery) {
    	
    /** CHANGE NOTHING ABOVE HERE */
    	
    	/** Initialize the MailPool */
    	AdvancedMailPool advancedMailPool = new AdvancedMailPool();
    	mailPool = advancedMailPool;
    	
        /** Initialize the MailSorter */
    	IMailSorter sorter = new AdvancedMailSorter(advancedMailPool);
    	
    /** CHANGE NOTHING BELOW HERE */
    	
    	/** Initialize robot */
    	robot = new Robot(sorter, delivery);
    	
    }
    
}
