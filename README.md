# Robustness-Testing
Robustness Testing was built with inspiration from techniques learnt in our 50.003 Elements of Software Construction module. This includes the use of ExecutorService Threadpool method to spawn multiple worker threads to stress a particular server endpoint, and Java Phaser to sequentially sent requests to the numerous endpoints at each phase. This allows us to measure the time taken for response and check for the number of packet hits and misses.

On average we tested each endpoint with 50 threads (simulating 50 different users) to stress the system and on average we achieve 75% of packets hit with an average of 3 seconds for the server to respond.
