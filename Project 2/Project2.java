import java.util.concurrent.Semaphore;
import java.util.*;

// used barbershop example in textbook for reference
// Proj2 class = Post Office Simulator

public class Project2 {
    static final int max_customers = 50;
    static final int max_workers = 3;

    public static Semaphore inside = new Semaphore(10, true); // only 10 can be inside
    public static Semaphore workers = new Semaphore(3, true); // 3 workers
    public static Semaphore mutex = new Semaphore(1, true); // mutex (mutual exclusion)
    public static Semaphore mutex2 = new Semaphore(1, true); // mutex (mutual exclusion)
    public static Semaphore coord = new Semaphore(3, true); // coordination
    public static Semaphore scale = new Semaphore(1, true); // shared resource
    public static Semaphore custReady = new Semaphore(0, true); // count # of ready customers
    public static Semaphore[] finished = new Semaphore[max_customers]; // finished customers array
    public static Semaphore leaveWorker = new Semaphore(0, true); // customer leaves
    public static Semaphore assignWorker = new Semaphore(0, true); // assign worker
    public static Semaphore custAskedWorker = new Semaphore(0, true); // check if the customer asked the worker before
                                                                      // proceeding with task

    public static Queue<Integer> queueCust = new LinkedList<>(); // list for customers (add to queue for order)
    public static Customer[] customersInfo = new Customer[max_customers]; // array for keep track of task and worker

    public static void main(String args[]) {

        Thread postalWorkers[] = new Thread[max_workers];
        Thread customers[] = new Thread[max_customers];

        // set all semaphores in finished customers array to 0
        for (int i = 0; i < max_customers; i++) {
            finished[i] = new Semaphore(0, true);
        }

        // create threads: postal workers
        for (int i = 0; i < max_workers; i++) {
            PostalWorker currentWorker = new PostalWorker(i);
            postalWorkers[i] = new Thread(currentWorker);
            postalWorkers[i].start();
        }

        // create threads: customers
        for (int i = 0; i < max_customers; i++) {
            Customer currentCustomer = new Customer(i);
            customersInfo[i] = currentCustomer; // keep track of task and worker
            customers[i] = new Thread(currentCustomer);
            customers[i].start();
        }

        // join customers
        for (int i = 0; i < max_customers; i++) {
            try {
                customers[i].join();
                System.out.println("Joined customer " + i);
            } catch (InterruptedException e) {
                System.out.println("error in customer join");
            }
        }

        System.exit(0); // end

    }

    // postal worker class
    static class PostalWorker implements Runnable {
        public int workerNum;
        public int currentCustomer;
        String task = "";

        // create a worker: 1-3 --> consturcotr
        PostalWorker(int workerNum) {
            this.workerNum = workerNum;

            // create postal worker
            System.out.println("Postal worker " + workerNum + " created");
        }

        // getter method for worker number
        public int getWorker() {
            return workerNum;
        }

        // setter method for worker number
        public void setWorker(int workerNum) {
            this.workerNum = workerNum;
        }

        // getter method for customer number
        public int getCustomer() {
            return currentCustomer;
        }

        // setter method for customer number
        public void setCustomer(int currentCustomer) {
            this.currentCustomer = currentCustomer;
        }

        // wait for semaphore
        public void wait(Semaphore temp) {
            try {
                temp.acquire();
            } catch (InterruptedException e) {

            }
        }

        // release semaphore
        public void signal(Semaphore temp) {
            temp.release();
        }

        // print function
        public void print() {
            System.out.println("Postal worker " + workerNum + " serving Customer " + currentCustomer);
        }

        @Override
        public void run() {

            while (true) {

                wait(Project2.custReady); // wait for customers who are ready
                wait(Project2.mutex); // mutual exclusion
                currentCustomer = Project2.queueCust.remove(); // dequeue: get customer number
                print(); // print that worker is serving a customer
                customersInfo[currentCustomer].setWorker(workerNum); // assign a worker to customer
                signal(Project2.assignWorker); // signal assigned worker
                signal(Project2.mutex); // signal mutex
                wait(Project2.coord); // wait for coord
                wait(Project2.custAskedWorker); // wait for customer to ask worker to perform task
                task = customersInfo[currentCustomer].getTask(); // get task
                taskPerformed(task); // thread sleep --> perform task
                signal(Project2.coord); // signal coord
                signal(Project2.finished[currentCustomer]); // customer is done
                wait(Project2.leaveWorker); // check if customer left
                signal(Project2.workers); // signal workers
            }
        }

        // thread sleeping + performing task
        public void taskPerformed(String task) {
            if (task.equals("buy stamps")) {

                // sleep for 1 second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("error in taskPerformed: 1");
                }
            } else if (task.equals("mail a letter")) {

                // sleep for 1.5 seconds
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    System.out.println("error in taskPerformed: 2");
                }
            } else if (task.equals("mail a package")) {
                scaleUse();
            }

            System.out.println("Postal worker " + workerNum + " finished serving customer " + currentCustomer);
        }

        // coordinate scale: only one worker at one time
        public void scaleUse() {
            // wait for scale
            wait(Project2.scale);

            // print worker is using scale
            System.out.println("Scales in use by postal worker " + workerNum);

            // sleep for 2 seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("error in scaleUse");
            }

            // signal scale
            signal(Project2.scale);
            System.out.println("Scales released by postal worker " + workerNum);
        }

    }

    // customer class
    static class Customer implements Runnable {
        public int customerNum;
        String taskOfCust = "";
        int currentWorker;

        // create a customer: 1-50 --> constructor
        // create customer, print string, assign task
        Customer(int customerNum) {

            // mutex2 --> mutual exclusion
            wait(Project2.mutex2);

            this.customerNum = customerNum;
            String temp = "";

            // create customer
            System.out.println("Customer " + customerNum + " created");

            // set task
            setTask(temp);

            // signal mutex2
            signal(Project2.mutex2);
        }

        // getter method for worker number
        public int getWorker() {
            return currentWorker;
        }

        // setter method for worker number
        public void setWorker(int currentWorker) {
            this.currentWorker = currentWorker;
        }

        // setter method for customer task
        public void setTask(String task) {
            task = taskNum(customerNum);
            this.taskOfCust = task;
        }

        // getter method for customer task
        public String getTask() {
            return taskOfCust;
        }

        // wait for semaphore
        public void wait(Semaphore temp) {
            try {
                temp.acquire();
            } catch (InterruptedException e) {

            }
        }

        // release semaphore
        public void signal(Semaphore temp) {
            temp.release();
        }

        // print statement according to number
        public void print(int num) {
            switch (num) {
                case 1:
                    System.out.println("Customer " + customerNum + " enters post office");
                    break;
                case 2:
                    System.out
                            .println("Customer " + customerNum + " asks postal worker "
                                    + customersInfo[customerNum].getWorker()
                                    + " to " + getTask());
                    break;
                case 3:
                    System.out.println("Customer " + customerNum + " finished " + taskCompletedString(taskOfCust));
                    break;
                case 4:
                    System.out.println("Customer " + customerNum + " leaves post office");
                    break;
            }
        }

        @Override
        public void run() {

            wait(Project2.inside); // wait for inside capacity
            print(1); // enter post office if there is space
            wait(Project2.mutex); // mutual exclusion
            Project2.queueCust.add(customerNum); // add customer to queue
            signal(Project2.custReady); // signal ready customer
            signal(Project2.mutex); // signal mutex
            wait(Project2.assignWorker); // check if worker is assigned
            print(2); // print customer asks worker for help with task
            signal(Project2.custAskedWorker); // signal that customer asked worker
            wait(Project2.finished[customerNum]); // wait for finished customer
            print(3); // print customer completed task
            signal(Project2.leaveWorker); // leave
            print(4); // print customer left
            signal(Project2.inside); // signal inside capacity
        }

        // source: https://mkyong.com/java/java-generate-random-integers-in-a-range/
        // assign task and return it
        public String taskNum(int customerNum) {
            String task = "";

            // get random number for task
            Random rand = new Random();
            int taskChoice = rand.nextInt(3) + 1;

            // assign task
            switch (taskChoice) {
                case 1:
                    task = "buy stamps";
                    break;
                case 2:
                    task = "mail a letter";
                    break;
                case 3:
                    task = "mail a package";
                    break;
                default:
                    task = "error";
                    break;
            }

            // return task
            return task;
        }

        // return completed task string to be printed
        public String taskCompletedString(String task) {

            if (task.equals("buy stamps")) {
                return "buying stamps";
            } else if (task.equals("mail a letter")) {
                return "mailing a letter";
            } else if (task.equals("mail a package")) {
                return "mailing a package";
            }

            // return empty if there is an error
            return "";
        }

    }
}
