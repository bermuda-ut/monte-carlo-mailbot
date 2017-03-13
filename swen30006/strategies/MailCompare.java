package strategies;

import automail.Building;
import automail.Clock;
import automail.MailItem;

import java.util.Comparator;

import static java.lang.Math.abs;

/**
 * Created by noxm on 13/03/17.
 */
public class MailCompare implements Comparator<MailItem> {
    private static double PRIORITY_FACTOR = 2.0;
    private static double TIME_FACTOR = 80.0;
    private static double DIST_FACTOR = 30.0;

    public static int getMailIntPriority(MailItem mailItem) {
        String priorityLevel = mailItem.getPriorityLevel();

        for(int i = 0; i < MailItem.PRIORITY_LEVELS.length; i++) {
            if (priorityLevel.equals(MailItem.PRIORITY_LEVELS[i])) {
                return i + 1;
            }
        }

        return 0;
    }

    public static double getScore(MailItem mailItem) {
        double timeRemain = mailItem.getArrivalTime() - Clock.Time();
        int dist = abs(Building.MAILROOM_LOCATION - mailItem.getDestFloor()) + 1;

//        System.out.println("TIME " + TIME_FACTOR / timeRemain);
//        System.out.println("PRIO " + PRIORITY_FACTOR * getMailIntPriority(mailItem));
//        System.out.println("DISt " + DIST_FACTOR / dist);
        return TIME_FACTOR / timeRemain + getMailIntPriority(mailItem) * PRIORITY_FACTOR + DIST_FACTOR / dist;
    }

    @Override
    public int compare(MailItem a, MailItem b) {
        double aScore = getScore(a);
        double bScore = getScore(b);

        if(aScore < bScore)
            return 1;
        else if (aScore > bScore)
            return -1;
        return 0;
    }
}
