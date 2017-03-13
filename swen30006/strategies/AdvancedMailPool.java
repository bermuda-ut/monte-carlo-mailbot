package strategies;

import automail.*;

import java.util.*;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailPool implements IMailPool {
    List<MailItem> mailPool;

    public static int MAX_CAPACITY = 4; // because its not static in StorageTube!
    public static double MIN_SCORE = 0.6;

    public AdvancedMailPool() {
        mailPool = new ArrayList<>();
    }

    public List<MailItem> getMails() {
        // get most effective combination
        List<List<MailItem>> combinations = new ArrayList<>();
        for(MailItem mailItem : mailPool) {
            List<MailItem> currList = new ArrayList<>();
            currList.add(mailItem);
            combinations.add(currList);
        }

        for(int i = 0; i < MAX_CAPACITY; i++) {
            List<List<MailItem>> branch = new ArrayList<>();

            for(List<MailItem> combination: combinations) {
                int totalSize = totalSize(combination);
                boolean branchedOut = false;

                if(totalSize < MAX_CAPACITY) {
                    for(MailItem mailItem: mailPool) {
                        // if not in the pool already
                        if(combination.indexOf(mailItem) < 0) {
                            List<MailItem> newCombination = new ArrayList<>();
                            newCombination.addAll(combination);
                            newCombination.add(mailItem);
                            // add the combination
                            if(totalSize(newCombination) <= MAX_CAPACITY) {
                                branchedOut = true;
                                branch.add(newCombination);
                            }
                        }
                    }
                }

                if(!branchedOut)
                    branch.add(combination);
            }

            combinations = branch;
        }

        // get most efficient combination
        double maxScore = 0;
        List<MailItem> curr = combinations.get(0);

        for(List<MailItem> combination: combinations) {
            double score = combinationScore(combination);
            if(score > maxScore) {
                curr = combination;
                maxScore = score;
            }
        }

        if(Clock.Time() < Clock.LAST_DELIVERY_TIME && maxScore < MIN_SCORE) {
            return Collections.EMPTY_LIST;
        }

        mailPool.removeAll(curr);
        return curr;
    }

    private double combinationScore(List<MailItem> mailList) {
        int sumPriority = 0;
        int maxDest = 0;

        int maxGap = maxGap(mailList) + 1;

        for(MailItem mailItem: mailList) {
            sumPriority += MailCompare.getMailIntPriority(mailItem);

            if(mailItem.getDestFloor() > maxDest)
                maxDest = mailItem.getDestFloor();
        }

        double finalScore = 1.0 * (sumPriority * 10) / (maxGap * 2) / (maxDest * 0.5) * totalSize(mailList) / MAX_CAPACITY;
        if(finalScore > MIN_SCORE) {
            System.out.println(finalScore);
        }
        return finalScore;
    }

    private int maxGap(List<MailItem> mailList) {
        int min = 1;
        int max = 1;

        for(MailItem mailItem : mailList) {
            int floor = mailItem.getDestFloor();
            if(floor > max)
                max = floor;
            if(floor < min)
                min = floor;
        }

        return max - min;
    }

    private int totalSize(List<MailItem> mailList) {
        int total = 0;
        for(MailItem mailItem : mailList) {
            total += mailItem.getSize();
        }
        return total;
    }

    @Override
    public void addToPool(MailItem mailItem) {
        mailPool.add(mailItem);
    }

    public boolean isEmptyPool() {
        if(mailPool.size() > 0)
            return false;
        return true;
    }

    public int size() {
        return mailPool.size();
    }
}
