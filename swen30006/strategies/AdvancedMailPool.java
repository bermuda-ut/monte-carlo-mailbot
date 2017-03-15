package strategies;

import automail.*;

import java.util.*;

import static java.lang.Math.abs;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailPool implements IMailPool {
    List<MailItem> mailPool;

    public static final int MAX_CAPACITY = (new StorageTube()).MAXIMUM_CAPACITY; // because its not static in StorageTube!

    public static final int MAX_DEPTH = 4;
    public static final int MAX_BRANCHES = 20;
    public static final int OVERSHOT = 10000;

    private static final double SUM_PRIORITY_FACTOR = 0.05;
    private static final double MAX_GAP_FACTOR = 1.25;
    private static final double MAX_DEST_FACTOR = 1.3;
    private static final double RATIO_FACTOR = 0.1;
    private static final double EFFIC_FACTOR = 0.3;

    public AdvancedMailPool() {
        mailPool = new ArrayList<>();
    }

    /***
     * Derieved from how priority is converted into double values
     * @param mailItem
     * @return priority in double
     */
    public static double getMailPriorityDouble(MailItem mailItem) {
        switch(mailItem.getPriorityLevel()){
            case "LOW":
                return 1;
            case "MEDIUM":
                return 1.5;
            case "HIGH":
                return 2;
        }
        return 0;
    }

    /***
     * For sorting mails by destination floor
     */
    class MailItemComparator implements Comparator<MailItem> {
        @Override
        public int compare(MailItem a, MailItem b) {
            if(a.getDestFloor() < b.getDestFloor())
                return -1;
            else if(a.getDestFloor() > b.getDestFloor())
                return 1;
            return 0;
        }
    }

    /***
     * Get list of mails to deliver
     * @return list of mails
     */
    public List<MailItem> getMails() {
        // get most effective combination
        List<List<MailItem>> combinations = new ArrayList<>();
        Stack<List<MailItem>> stack = new Stack<>();
        int branchCount = Math.min(MAX_BRANCHES, mailPool.size());
        int depth = Math.min(MAX_DEPTH, MAX_CAPACITY);

        Collections.shuffle(mailPool);
        for(int m = 0; m < branchCount; m++) {
            List<MailItem> currList = new ArrayList<>();
            currList.add(mailPool.get(m));
            stack.add(currList);
        }
        Collections.shuffle(stack);

        while(stack.size() > 0) {
            List<MailItem> currCombination = stack.pop();

            currCombination.sort(new MailItemComparator());

            double currScore = getEfficiency(currCombination);
            boolean modified = false;

            if(currCombination.size() >= depth) {
                combinations.add(currCombination);
                continue;
            }

            Collections.shuffle(mailPool);
            for(int i = 0; i < branchCount; i++) {
                MailItem toAdd = mailPool.get(i);

                boolean effective = false;
                if((toAdd.getDestFloor() > Building.MAILROOM_LOCATION && currCombination.get(0).getDestFloor() > Building.MAILROOM_LOCATION) ||
                   (toAdd.getDestFloor() < Building.MAILROOM_LOCATION && currCombination.get(0).getDestFloor() < Building.MAILROOM_LOCATION))
                    effective = true;

                if(!effective)
                    continue;

                // not found in current combination
                if(currCombination.indexOf(toAdd) < 0) {
                    List<MailItem> newCombination = new ArrayList<>(currCombination);
                    newCombination.add(toAdd);
                    newCombination.sort(new MailItemComparator());

                    double newScore = getEfficiency(newCombination);
                    if(newScore >= currScore && totalSize(newCombination) <= MAX_CAPACITY) {
                        stack.add(newCombination);
                        modified = true;
                    }
                }
            }

            if(!modified) {
                combinations.add(currCombination);
            }
        }

        /*
        // generate tree and get the final leaves
        int depth = (MAX_CAPACITY < MAX_DEPTH) ? MAX_CAPACITY : MAX_DEPTH;

        for(int i = 0; i < depth; i++) {
            List<List<MailItem>> branch = new ArrayList<>();

            for(List<MailItem> combination: combinations) {
                int totalSize = totalSize(combination);
                boolean branchedOut = false;

                if(totalSize < MAX_CAPACITY) {
                    int branchingFactor = (mailPool.size() < MAX_BRANCHING_FACTOR)? mailPool.size(): MAX_BRANCHING_FACTOR;
                    int trial = 0;

                    for(int j = 0; j < branchingFactor; j++) {
                        int randomNum = ThreadLocalRandom.current().nextInt(0, mailPool.size());
                        MailItem mailItem = mailPool.get(randomNum);

                        // if not in the pool
                        if(combinationScore(combination) >= MIN_SCORE) {
                            continue;
                        }

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
        */

        // get most efficient combination
        double maxScore = 0;
        List<MailItem> curr = combinations.get(0);

        for(List<MailItem> combination: combinations) {
            double score = getEfficiency(combination);
            if(score > maxScore) {
                curr = combination;
                maxScore = score;
            }
        }

        mailPool.removeAll(curr);
//        System.out.println("");
//        System.out.println(curr);
//        System.out.println("EXPECT " + simulateDeliveryScore(curr));
//        System.out.println("STEPS " + getSteps(curr));
        return curr;
    }

    /***
     * Minium destination floor for the given mail list
     * @param mailList list of mails to deliver
     * @return minimum floor level
     */
    private int minFloor(List<MailItem> mailList) {
        int min = mailList.get(0).getDestFloor();
        for(MailItem mailItem : mailList) {
            if(min > mailItem.getDestFloor())
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
        for(MailItem mailItem : mailList) {
            if(max < mailItem.getDestFloor())
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
        for(MailItem deliveryItem: deliveryItems) {
            // Travel time
            currTime += Math.abs(currFloor - deliveryItem.getDestFloor());

            // deliver scorend time
            double priority_weight = getMailPriorityDouble(deliveryItem);
            score += Math.pow(currTime - deliveryItem.getArrivalTime(),penalty)*priority_weight;
            currTime += 1;
            currFloor = deliveryItem.getDestFloor();
        }

        return score;
    }

    /**
     * Calculate efficiency for the given list of mails to deliver
     * efficiency = estimatedScore / steps = score per steps
     * @param mailItemList
     * @return efficiency score
     */
    private double getEfficiency(List<MailItem> mailItemList) {
        int steps = getSteps(mailItemList);
        double score = simulateDeliveryScore(mailItemList);

        return 1.0 * (score/steps);
    }

    /***
     * Calculate time units required to deliver the mails
     * @param mailList list of mails to deliver
     * @return steps
     */
    private int getSteps(List<MailItem> mailList) {
        // time units required to finish delivering
        int steps = mailList.size();

        if(maxFloor(mailList) > Building.MAILROOM_LOCATION)
            steps += Math.abs(maxFloor(mailList) - Building.MAILROOM_LOCATION) * 2;
        if(minFloor(mailList) < Building.MAILROOM_LOCATION)
            steps += Math.abs(maxFloor(mailList) - Building.MAILROOM_LOCATION) * 2;

        return steps;
    }

    private double combinationScore(List<MailItem> mailList) {
        int sumPriority = 0;
        int maxGap = maxGap(mailList) + 1;

        int maxFloor = maxFloor(mailList);
        int minFloor = minFloor(mailList);
        int minFactor = Math.min(minFloor, Building.MAILROOM_LOCATION);
        int maxFactor = Math.max(maxFloor, Building.MAILROOM_LOCATION);
        int maxDest = maxFactor - minFactor;

        double sumScore = SUM_PRIORITY_FACTOR * sumPriority;
        double gapScore = MAX_GAP_FACTOR / maxGap;
        double destScore = MAX_DEST_FACTOR / maxDest;
        double ratiScore = RATIO_FACTOR * totalSize(mailList) / MAX_CAPACITY;
        double efficiency = EFFIC_FACTOR * getEfficiency(mailList);

        double finalScore = sumScore + gapScore + destScore + ratiScore;
        return finalScore * efficiency;
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
        if(mailPool.size() > 0)
            return false;
        return true;
    }

    public int size() {
        return mailPool.size();
    }
}
