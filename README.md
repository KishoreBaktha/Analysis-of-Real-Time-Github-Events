# Analysis-of-Real-Time-Github-Events
Apache Kafka
In this project, we are going to access the data of public repositories uploaded in Github to analyse and find out interesting
trends such as the top programming languages used and  top ten events or the top ten actors by the count of events.

The data can be collected using the Github api provided and we can analyse the data which is obtained in the form of JSON 
format to find out interesting trends. The main source of data for this project would be the GitHub API for developers
(https://developer.github.com/v3/).

Apache Flink was used for processing of streaming data. Flink provides low latency and high throughput for data generated 
continuously. Elasticsearch is a distributed, open-source search and analytics engine which we use for storing data as an index
. It supports different types of data and has high speed and scalability. Kibana is an open-source visualisation engine plugin
for elasticsearch. You can visualise different types of charts and create a dashboard for monitoring the stream of data.
Data from the GitHub API and the public dataset will be used to generate a stream of events, which will be ingested by Apache
Flink for processing. The processed data will then be written to Elasticsearch index and Kibana will connect to that index 
for querying and visualizing the data in real-time. 



