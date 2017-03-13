package strategies;

import automail.Clock;
import automail.IMailSorter;
import automail.MailItem;
import automail.StorageTube;
import exceptions.TubeFullException;

import java.util.List;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailSorter implements IMailSorter {

    AdvancedMailPool advancedMailPool;

    public AdvancedMailSorter(AdvancedMailPool advancedMailPool) {
        this.advancedMailPool = advancedMailPool;
    }

    @Override
    public boolean fillStorageTube(StorageTube tube) {

//    	System.out.println("Mail Items Left: "+ advancedMailPool.size());

        try{
            int initialDestination = -1;

            if (!advancedMailPool.isEmptyPool()) {
                List<MailItem> mailItems = advancedMailPool.getMails();
                for(MailItem mailItem : mailItems) {
                    tube.addItem(mailItem);
                }

                if(mailItems.size() > 0)
                    return true;
                else
                    return false;
            }
        } catch(TubeFullException e){
            return true;
        }

        /**
         * Handles the case where the last delivery time has elapsed and there are no more
         * items to deliver.
         */
        if(Clock.Time() > Clock.LAST_DELIVERY_TIME && advancedMailPool.isEmptyPool() && !tube.isEmpty()){
            return true;
        }

        return false;
    }
}
