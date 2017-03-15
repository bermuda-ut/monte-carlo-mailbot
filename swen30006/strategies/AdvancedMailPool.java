package strategies;

import automail.*;

import java.util.*;

/**
 * Smart mail pool that returns efficient combination to put into the tube
 */
public class AdvancedMailPool implements IMailPool {
    // Would love to use the one defined in StorageTube but its not static in StorageTube!
    private static final int MAX_CAPACITY = (new StorageTube()).MAXIMUM_CAPACITY;
    private static final int MAX_DEPTH = 4;     // Maximum depth of the tree
    private static final int MAX_BRANCHES = 17; // Maximum branching factor of the tree
    private static final int OVERSHOT = 1000;   // Used for overestimating the estimated score

    // Redefined here because in MailItem, they are defined under an array therefore not a strict constant.
    // Must be a constant to be used in switch/case statement
    private static final String LOW_PRIO_TAG = "LOW";
    private static final String MED_PRIO_TAG = "MEDIUM";
    private static final String HIGH_PRIO_TAG = "HIGH";

    List<MailItem> mailPool;

    public AdvancedMailPool() {
        mailPool = new ArrayList<>();
    }

    /***
     * Derieved from how priority is converted into double values
     * @param mailItem
     * @return priority in double
     */
    public static double getMailPriorityDouble(MailItem mailItem) {
        // copied from existing code
        switch (mailItem.getPriorityLevel()) {
            case LOW_PRIO_TAG:
                return 1;
            case MED_PRIO_TAG:
                return 1.5;
            case HIGH_PRIO_TAG:
                return 2;
        }
        return 0;
    }

    /***
     * Get a efficient combination of mails to deliver using tree and utilizing MonteCarlo-style Search
     * @return list of mails
     */
    public List<MailItem> getMails() {
        List<List<MailItem>> combinations = new ArrayList<>(); // final leaves
        Stack<List<MailItem>> stack = new Stack<>();
        int branchCount = Math.min(MAX_BRANCHES, mailPool.size());
        int depth = Math.min(MAX_DEPTH, MAX_CAPACITY);

        // select random branches to travers down to and add to stack
        Collections.shuffle(mailPool, new Random(mailPool.size()));
        for (int m = 0; m < branchCount; m++) {
            List<MailItem> currList = new ArrayList<>();
            currList.add(mailPool.get(m));
            stack.add(currList);
        }

        while (stack.size() > 0) {
            // get node. at this point, the combination is sorted by destination
            List<MailItem> currCombination = stack.pop();

            double currScore = getEfficiency(currCombination);
            boolean modified = false;

            // depth limit reached, do not traverse more
            if (currCombination.size() >= depth) {
                combinations.add(currCombination);
                continue;
            }

            // randomize and choose a mail to add to the combination
            Collections.shuffle(mailPool, new Random(stack.size()));
            for (int i = 0; i < branchCount; i++) {
                MailItem toAdd = mailPool.get(i);

                // logic: if destination you are going to go is above and below mail room, you might aswell re-pick up
                // the mails since you are guaranteed to enter the mailroom again
                // The following statement prevents such combination
                if (!(toAdd.getDestFloor() > Building.MAILROOM_LOCATION &&
                        currCombination.get(0).getDestFloor() > Building.MAILROOM_LOCATION) &&
                        !(toAdd.getDestFloor() < Building.MAILROOM_LOCATION &&
                                currCombination.get(0).getDestFloor() < Building.MAILROOM_LOCATION))
                    continue;

                // not found in current combination
                if (currCombination.indexOf(toAdd) < 0) {
                    List<MailItem> newCombination = new ArrayList<>(currCombination);
                    newCombination.add(toAdd);
                    newCombination.sort(new MailItemComparator());

                    // only traverse down the graph if the score is increasing and is below maximum capacity
                    double newScore = getEfficiency(newCombination);
                    if (newScore >= currScore && totalSize(newCombination) <= MAX_CAPACITY) {
                        stack.add(newCombination);
                        modified = true;
                    }
                }
            }

            // when anything below current leaf is most likely to be lower, just add current leaf
            if (!modified) {
                combinations.add(currCombination);
            }
        }

        // get most efficient combination
        double maxScore = 0;
        List<MailItem> curr = combinations.get(0);

        for (List<MailItem> combination : combinations) {
            combination.sort(new MailItemComparator());
            double score = getEfficiency(combination);

            if (score > maxScore) {
                curr = combination;
                maxScore = score;
            }
        }

        mailPool.removeAll(curr);
        return curr;
    }

    /***
     * Minium destination floor for the given mail list
     * @param mailList list of mails to deliver
     * @return minimum floor level
     */
    private int minFloor(List<MailItem> mailList) {
        int min = mailList.get(0).getDestFloor();
        for (MailItem mailItem : mailList) {
            if (min > mailItem.getDestFloor())
                min = mailItem.getDestFloor();
        }

        return min;
    }

    /***
     * Maximum destination floor for the given mail list
     * @param mailList list of mails to deliver
     * @return maximum floor level
     */
    private int maxFloor(List<MailItem> mailList) {
        int max = mailList.get(0).getDestFloor();
        for (MailItem mailItem : mailList) {
            if (max < mailItem.getDestFloor())
                max = mailItem.getDestFloor();
        }
        return max;
    }

    /***
     * Calculate expected delivery score upon deliverying the list of mails
     * @param deliveryItems list of mails to deliver
     * @return expected score gain with overhead
     */
    private double simulateDeliveryScore(List<MailItem> deliveryItems) {
        final double penalty = 1.1;
        int currTime = Clock.Time() + OVERSHOT; // overhead estimation
        int currFloor = Building.MAILROOM_LOCATION;
        double score = 0;

        // Determine the priority_weight
        for (MailItem deliveryItem : deliveryItems) {
            // Travel time
            currTime += Math.abs(currFloor - deliveryItem.getDestFloor());

            // deliver and add time
            double priority_weight = getMailPriorityDouble(deliveryItem);
            score += Math.pow(currTime - deliveryItem.getArrivalTime(), penalty) * priority_weight;
            currTime += 1;
            currFloor = deliveryItem.getDestFloor();
        }

        return score;
    }

    /**
     * Calculate efficiency for the given list of mails to deliver
     * efficiency = estimatedScore / steps = score per steps
     *
     * @param mailItemList
     * @return efficiency score
     */
    private double getEfficiency(List<MailItem> mailItemList) {
        int steps = getSteps(mailItemList);
        double score = simulateDeliveryScore(mailItemList);

        return 1.0 * (score / steps);
    }

    /***
     * Calculate time units required to deliver the mails
     * @param mailList list of mails to deliver
     * @return steps
     */
    private int getSteps(List<MailItem> mailList) {
        // time units required to finish delivering
        int steps = mailList.size();

        if (maxFloor(mailList) > Building.MAILROOM_LOCATION)
            steps += Math.abs(maxFloor(mailList) - Building.MAILROOM_LOCATION) * 2;
        if (minFloor(mailList) < Building.MAILROOM_LOCATION)
            steps += Math.abs(minFloor(mailList) - Building.MAILROOM_LOCATION) * 2;

        return steps;
    }

    /***
     * Get total size of the current list of mails
     * @param mailList list of mails to consider
     * @return sum of sizes
     */
    private int totalSize(List<MailItem> mailList) {
        int total = 0;
        for (MailItem mailItem : mailList) {
            total += mailItem.getSize();
        }
        return total;
    }

    /***
     * Add mail to the pool
     * @param mailItem the mail item being added.
     */
    @Override
    public void addToPool(MailItem mailItem) {
        mailPool.add(mailItem);
    }

    /***
     * Check if the pool is empty or not
     * @return true/false
     */
    public boolean isEmptyPool() {
        if (mailPool.size() > 0)
            return false;
        return true;
    }

    /***
     * Get size of the mail pool
     * @return size of the mail pool
     */
    public int size() {
        return mailPool.size();
    }

    /***
     * For sorting mails by destination floor
     */
    class MailItemComparator implements Comparator<MailItem> {
        @Override
        public int compare(MailItem a, MailItem b) {
            if (a.getDestFloor() < b.getDestFloor())
                return -1;
            else if (a.getDestFloor() > b.getDestFloor())
                return 1;
            return 0;
        }
    }
}
