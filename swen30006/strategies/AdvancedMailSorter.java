package strategies;

import automail.IMailSorter;
import automail.MailItem;
import automail.StorageTube;
import exceptions.TubeFullException;

import java.util.List;

/**
 * Recieves mails from AdvancedMailPool and puts them into the tube for the robot
 */
public class AdvancedMailSorter implements IMailSorter {
    private AdvancedMailPool advancedMailPool;

    public AdvancedMailSorter(AdvancedMailPool advancedMailPool) {
        this.advancedMailPool = advancedMailPool;
    }

    /***
     * Fill the tube with effective combination of mails to deliver
     * @param tube tube to fill
     * @return true/false - whether the robot is ready to leave
     */
    @Override
    public boolean fillStorageTube(StorageTube tube) {
        // System.out.println("Mail Items Left: "+ advancedMailPool.size());
        try {
            if (!advancedMailPool.isEmptyPool()) {
                // get bunch of mails to deliver and add them all to the tube
                List<MailItem> mailItems = advancedMailPool.getMails();
                for (MailItem mailItem : mailItems) {
                    tube.addItem(mailItem);
                }

                // if stuff were actually added, send the robot!
                if (mailItems.size() > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (TubeFullException e) {
            // This won't happen due to the smart behaviour in advancedMailPool
            return true;
        }

        return false;
    }
}
