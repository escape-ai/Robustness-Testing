import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;

public class TestServerPhaser implements Callable<Double> {

    private Phaser phaser;
    private String url;

    public TestServerPhaser(Phaser phaser, String url){
        this.phaser = phaser;
        this.url = url;
    }
    public TestServerPhaser(){}

    public static void main(String[] args){
        List<String> listOfURLs = Arrays.asList("https://fakerinos.herokuapp.com/api/docs/", "https://fakerinos.herokuapp.com/api/docs/default",
                "https://fakerinos.herokuapp.com/api/articles/article/", "https://fakerinos.herokuapp.com/api/articles/deck/", "https://fakerinos.herokuapp.com/api/articles/tag/",
                "https://fakerinos.herokuapp.com/api/articles/domain/", "https://fakerinos.herokuapp.com/api/articles/domaintag/" ,"https://fakerinos.herokuapp.com/api/accounts/",
                "https://fakerinos.herokuapp.com/api/accounts/user/", "https://fakerinos.herokuapp.com/api/accounts/profile/", "https://fakerinos.herokuapp.com/api/accounts/player/");
        int numThreads = 50;
        ExecutorService tpExecutor = Executors.newFixedThreadPool(numThreads);
        Phaser ph = new Phaser();

        ph.register();
        double totalPercentHits = 0.0;
        int totalDrops = 0;

        for (int counter=0; counter<listOfURLs.size(); counter++) {
            Future<Double>[] timestamps = new FutureTask[numThreads];

            for (int i = 0; i < numThreads; i++) {
                timestamps[i] = tpExecutor.submit(new TestServerPhaser(ph, listOfURLs.get(counter)));
            }

            try {
                Thread.sleep(100);
            } catch (Exception e) {e.printStackTrace();}

            ph.arriveAndAwaitAdvance();
            if (ph.getPhase() != counter + 1 ) throw new AssertionError("Phase Counter is wrong");

            List<Double> valuesOf = new TestServerPhaser().getMean(timestamps);
            Double mean = valuesOf.get(0);
            Integer drops = valuesOf.get(1).intValue();
            double percentageHits = (numThreads-drops)*100/numThreads;
            totalDrops += drops;
            totalPercentHits += percentageHits;
            System.out.println("***** URL: " + listOfURLs.get(counter) + " ******");
            System.out.println("Average Time Taken(s): " + mean + "\tPercentage Hits(%): " + percentageHits + "\tPackets Dropped: " + drops);
        }
        System.out.println("\n\n***** Test Summary *****");
        System.out.println("Threads per URL: " + numThreads);
        System.out.println("Total threads spawned: " + numThreads * listOfURLs.size());
        System.out.println("Total Hits(%): " + totalPercentHits/listOfURLs.size() + "\nTotal Packets Dropped: " + totalDrops);
        System.out.println("***** TEST END *****");
        // Closing all threads
        ph.arriveAndDeregister();
        tpExecutor.shutdown();
    }

    public List<Double> getMean(Future<Double>[] listOf){
        Double totalTime = 0.0;
        int numberFailed = 0;
        for (Future f : listOf){
            try {
                Double time = (Double) f.get();
                if (time < 0.0){
                    numberFailed ++;
                }
                else{
                    totalTime += time;
                }
            }
            catch(Exception e){e.printStackTrace();}
        }
        return Arrays.asList((totalTime/listOf.length), Double.valueOf(numberFailed));
    }

    public Double call(){
        phaser.register();
        phaser.arriveAndAwaitAdvance();

        try{
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            long timeStart = System.nanoTime();
            connection.connect();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300){
                long timeEnd = System.nanoTime();
                phaser.arriveAndDeregister();
                return (timeEnd-timeStart)/(Math.pow(10.0,9.0));
            }
            else if(this.url.endsWith("/accounts/user/") || this.url.endsWith("/accounts/player/") || this.url.endsWith("/accounts/profile/")) {
                if (connection.getResponseCode() == 401) {
                    long timeEnd = System.nanoTime();
                    phaser.arriveAndDeregister();
                    return (timeEnd - timeStart) / (Math.pow(10.0, 9.0));
                }
            }
            else{
                phaser.arriveAndDeregister();
                return -1.0 * connection.getResponseCode();
            }
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }
}
