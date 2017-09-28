# Kademlia

This project is a faithfully implementation of the [Kademlia distributed hash table](https://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf) (DHT) routing algorithm. This is an implementation of the strict version of the algorithm. The code is readable, documented, well tested, and fills in all of the missing pieces from the algorithm presented in the original paper.

The Kademlia algorithm is the backbone of many massive-scale decentralized applications, including...
 * [Decentrialized BitTorrent (BitTorrent DHT)](http://www.bittorrent.org/beps/bep_0005.html)
 * [Tox](https://en.wikipedia.org/wiki/Tox_(protocol))
 * [IPFS](https://en.wikipedia.org/wiki/IPFS)
 * [Kad/eMule](https://en.wikipedia.org/wiki/Kad_Network)
 * [Gnutella/Limewire](https://en.wikipedia.org/wiki/Gnutella)

To use this implementation, construct an instance of the ```Router``` class. You can specify the main parameters of the routing algorithm here, specifically...
 * node ID bit length
 * number of branches to generate whenever a k-bucket splits
 * maximum number of nodes allowed in each k-bucket
 * maximum number of cache nodes allowed in each k-bucket

Once you have a router class, you can call...
 * `touch()` whenever a node comes in contact
 * `stale()` whenever a node has failed to come in contact / keep-alive
 * `find()` to find the closest nodes in the routing table to some ID

If you need usage examples, check out the `RouterTest.java` class in the test sources.