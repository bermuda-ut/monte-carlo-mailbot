package strategies;

import automail.Building;
import automail.Clock;
import automail.MailItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailList {
    private List<MailItem> mails;
    private int floor;
    private double priority;

    public AdvancedMailList(int floor) {
        this.floor = floor;
        priority = 0;
        mails = new ArrayList<>();
    }

    public void addMailItem(MailItem mailItem) {
        priority += MailCompare.getMailIntPriority(mailItem);
        mails.add(mailItem);
        Collections.sort(mails, new MailCompare());
    }

    public void removeMailItem (MailItem mailItem) {
        priority += MailCompare.getMailIntPriority(mailItem);
        mails.remove(mailItem);
    }

    public MailItem getMailItem(int maxSize) {
        // assume sorted by importance
        for(int i = 0; i < mails.size(); i++) {
            MailItem mail = mails.get(i);

            if(mail.getSize() <= maxSize) {
                return mail;
            }
        }

        // nothing can fit!
        return null;
    }

    private double getFloorScore(int idealDestination) {
        if(floor == idealDestination)
            return 1;

        return 1.0 / (1 + abs(Building.MAILROOM_LOCATION - floor));
    }

    private double getRemainderTime() {
        double time = Clock.LAST_DELIVERY_TIME;
        for(MailItem mail: mails) {
            double t = Clock.LAST_DELIVERY_TIME - mail.getArrivalTime();
            if(t < time)
                time = t;
        }
        return time;
    }

    public static double SIZE_FACTOR = 1;
    public static double BONUS_FACTOR = 1;
    public static double PRIORITY_FACTOR = 1;
    public static double DISTANCE_FACTOR = 10;

    public double getImportance(int idealDestination) {
        // get efficiency score

        if(mails.size() == 0)
            return 0;

        double bonus = 1;
        double distance = abs(Building.MAILROOM_LOCATION - floor) + 1;

        if(floor == idealDestination)
            bonus = BONUS_FACTOR;
//        System.out.println("---");
//        System.out.println("PRIO " + priority * PRIORITY_FACTOR);
//        System.out.println("SIZE " + mails.size() * SIZE_FACTOR);
//        System.out.println("DIST " + DISTANCE_FACTOR / distance);

        return (priority * PRIORITY_FACTOR + mails.size() * SIZE_FACTOR + DISTANCE_FACTOR / distance) * bonus;
    }

    public int size() {
        return mails.size();
    }

    public int getFloor() {
        return floor;
    }
}
