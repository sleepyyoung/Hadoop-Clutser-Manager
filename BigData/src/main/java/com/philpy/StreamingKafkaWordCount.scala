package com.philpy

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.sql.{Connection, DriverManager, PreparedStatement}

object StreamingKafkaWordCount {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "hadoop")
    // 创建SparkConf
    val conf = new SparkConf().setMaster("local[*]").setAppName("SparkStreamingKafkaWorkWordCount").set("spark.testing.memory", "2147480000")
    // 创建 Spark Streaming 上下文，设置批次间隔为1秒
    val ssc = new StreamingContext(conf, Seconds(1))
    // 设置检查点目录，因为需要用检查点记录历史批次处理的结果数据
    ssc.checkpoint("hdfs://centos01:8020/spark-ck")

    //设置输入流的 Kafka 主题，可以设置多个
    val kafkaTopics = Array("topictest", "topictest2")

    //Kafka配置属性
    val kafkaParams = Map[String, Object](
      //Kafka Broker 服务器的连接地址
      "bootstrap.servers" -> "centos01:9092,centos02:9092,centos03:9092",
      //设置反序列化key的程序类，与生产者对应
      "key.deserializer" -> classOf[StringDeserializer],
      //设置反序列化value的程序类，与生产者对应
      "value.deserializer" -> classOf[StringDeserializer],
      //设置消费者组ID，ID相同的消费者属于同一个消费者组
      "group.id" -> "1",
      //Kafka不提交自动偏移量（默认为true），由Spark管理
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val inputStream: InputDStream[ConsumerRecord[String, String]] =
      KafkaUtils.createDirectStream[String, String](
        ssc,
        LocationStrategies.PreferConsistent,
        Subscribe[String, String](kafkaTopics, kafkaParams)
      )

    //对接收到的一个DStream进行解析，取出消息记录的key和value
    val linesDStream = inputStream.map(record => (record.key, record.value))
    //默认情况下，消息内容存放在value中，取出value的值
    val wordDStream = linesDStream.map(_._2)
    val word = wordDStream.flatMap(_.split(" "))
    val pair = word.map(x => (x, 1))

    //更新每个单词的数量，实现按批次累加
    val result: DStream[(String, Int)] = pair.updateStateByKey(updateFunc)
    //打印DStream中的元素到控制台
    result.print()


    //把DStream保存到MySQL数据库中
    /*
    mysql> create database spark;
    Query OK, 1 row affected (0.05 sec)

    mysql> use spark;
    Database changed
    mysql> create table wordcount(word varchar(100),count int);
    Query OK, 0 rows affected (0.05 sec)
     */

    result.foreachRDD(rdd => {
      def func(records: Iterator[(String, Int)]) {
        var conn: Connection = null
        var stmt: PreparedStatement = null
        try {
          val url = "jdbc:mysql://localhost:3306/spark"
          val user = "hive"
          val password = "hive"
          conn = DriverManager.getConnection(url, user, password)
          records.foreach(p => {
            val sql = "insert into wordcount(word,count) values (?,?)"
            stmt = conn.prepareStatement(sql)
            stmt.setString(1, p._1.trim)
            stmt.setInt(2, p._2.toInt)
            stmt.executeUpdate()
          })
        } catch {
          case e: Exception => e.printStackTrace()
        } finally {
          if (stmt != null) {
            stmt.close()
          }
          if (conn != null) {
            conn.close()
          }
        }
      }

      val repartitionedRDD = rdd.repartition(3)
      repartitionedRDD.foreachPartition(func)
    })

    //启动计算
    ssc.start()
    //等待计算结束
    ssc.awaitTermination()
  }

  /**
   * 定义状态更新函数，按批次累加单词数量
   *
   * @param values 当前批次单词出现的次数，相当于 Seq(1, 1, 1)
   * @param stats  上一批次累加的结果，因为有可能没有值，所以用Option类型
   */
  val updateFunc: (Seq[Int], Option[Int]) => Some[Int] = (values: Seq[Int], state: Option[Int]) => {
    //累加当前批次单词的数量
    val currentCount = values.sum
    //获取上一批单词的数量，默认值为0
    val previousCount = state.getOrElse(0)
    //求和
    Some(currentCount + previousCount)
  }
}
