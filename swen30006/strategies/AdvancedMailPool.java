package strategies;

import automail.*;

import java.util.*;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailPool implements IMailPool {
    HashMap<Integer, AdvancedMailList> mailPool;

    public static int MAX_CAPACITY = 4; // because its not static in StorageTube!!!!!!!!

    public AdvancedMailPool() {
        mailPool = new HashMap<>();

        // generate list for each priority
        for(int i = Building.LOWEST_FLOOR; i < Building.FLOORS + 1; i++) {
            mailPool.put(i, new AdvancedMailList(i));
        }
    }

    public MailItem getMail(int totalOfSizes, int idealDestination) {
        int maxSize = MAX_CAPACITY - totalOfSizes;

        List<AdvancedMailList> mailLists = new ArrayList<>();
        for(AdvancedMailList mailList: mailPool.values()) {
            mailLists.add(mailList);
        }

        Collections.sort(mailLists, new MailListCompare(idealDestination));

        for(AdvancedMailList mailList: mailLists) {
            MailItem mail = mailList.getMailItem(maxSize);
            if(mail != null) {
                mailList.removeMailItem(mail);
                return mail;
            }
        }

        return null;
    }

    @Override
    public void addToPool(MailItem mailItem) {
        AdvancedMailList mailList = mailPool.get(mailItem.getDestFloor());
        mailList.addMailItem(mailItem);
//        System.out.println(mailList);
    }

    public boolean isEmptyPool() {
        for(AdvancedMailList mailList : mailPool.values()) {
            if(mailList.size() > 0)
                return false;
        }
        return true;
    }

    public int size() {
        int size = 0;
        for(AdvancedMailList mailList : mailPool.values()) {
            size += mailList.size();
        }
        return size;
    }
}
