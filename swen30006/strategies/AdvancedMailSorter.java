package strategies;

import automail.Clock;
import automail.IMailSorter;
import automail.MailItem;
import automail.StorageTube;
import exceptions.TubeFullException;

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

            while (!advancedMailPool.isEmptyPool() && !tube.isFull()) {
                MailItem mailItem = advancedMailPool.getMail(tube.getTotalOfSizes(), initialDestination);
//                System.out.println(mailItem);

                if(mailItem != null) {

                    if(initialDestination < 0)
                        initialDestination = mailItem.getDestFloor();

                    tube.addItem(mailItem);
                } else {
                    return true;
                }
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
