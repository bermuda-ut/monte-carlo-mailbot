package strategies;

import java.util.Comparator;

/**
 * Created by noxm on 13/03/17.
 */
public class MailListCompare implements Comparator<AdvancedMailList> {
    private int idealDestination;

    public MailListCompare(int idealDestination) {
        this.idealDestination = idealDestination;
    }

    @Override
    public int compare(AdvancedMailList a, AdvancedMailList b) {
        if(a.getImportance(idealDestination) < b.getImportance(idealDestination)) {
            return 1;
        } else if(a.getImportance(idealDestination) > b.getImportance(idealDestination)) {
            return -1;
        }
        return 0;
    }
}
