-- MySQL dump 10.13  Distrib 9.6.0, for macos15.7 (arm64)
--
-- Host: 127.0.0.1    Database: education1
-- ------------------------------------------------------
-- Server version	8.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `history`
--

DROP TABLE IF EXISTS `history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history` (
  `hid` varchar(255) DEFAULT NULL,
  `sid` varchar(255) DEFAULT NULL,
  `qid` varchar(255) DEFAULT NULL,
  `time` date DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `history`
--

LOCK TABLES `history` WRITE;
/*!40000 ALTER TABLE `history` DISABLE KEYS */;
INSERT INTO `history` VALUES ('1708225611191','1707103528830','1708225611104','2024-02-18','计算错误','正负值错误'),('1708263923047','1707103528830','1708263922484','2024-02-18','计算错误','正负值错误'),('1708307786196','1707103528830','1708307785703','2024-02-19','计算错误','正负值错误'),('1708308535319','1707103528830','1708308535157','2024-02-19','计算错误','正负值错误'),('1780455632402','1707103528830','1780455632293','2026-06-03','计算错误','正负值错误'),('1780455843569','1707103528830','1780455843549','2026-06-03','计算错误','正负值错误'),('1780487954941','1707103528830','1780487954891','2026-06-03','计算错误','正负值错误'),('1780488387471','1707103528830','1780488387415','2026-06-03','计算错误','正负值错误');
/*!40000 ALTER TABLE `history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_basic_info`
--

DROP TABLE IF EXISTS `question_basic_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_basic_info` (
  `sid` varchar(255) NOT NULL,
  `qid` varchar(255) NOT NULL,
  `questionType` varchar(255) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `wrongType` varchar(255) DEFAULT NULL,
  `wrongDetails` varchar(255) DEFAULT NULL,
  `mark` tinyint DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `questionText` varchar(255) DEFAULT NULL,
  `wrongText` varchar(716) DEFAULT NULL,
  PRIMARY KEY (`sid`,`qid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_basic_info`
--

LOCK TABLES `question_basic_info` WRITE;
/*!40000 ALTER TABLE `question_basic_info` DISABLE KEYS */;
INSERT INTO `question_basic_info` VALUES ('1707103528830','1708221325896','选择题','2024-02-18','数学','计算错误','正负值错误',0,NULL,'数学/方程问题',NULL,NULL),('1707103528830','1708225611104','选择题','2024-02-17','数学','计算错误','正负值错误',1,NULL,'',NULL,NULL),('1707103528830','1708263922484','选择题','2024-02-16','数学','计算错误','正负值错误',0,NULL,'',NULL,NULL),('1707103528830','1708307785703','选择题','2024-02-19','数学','计算错误','正负值错误',0,NULL,'',NULL,NULL),('1707103528830','1708308535157','选择题','2024-02-19','数学','计算错误','正负值错误',1,NULL,'',NULL,NULL),('1707103528830','1780455632293','选择题','2026-06-03','数学',NULL,NULL,0,'https://ai4education.oss-cn-beijing.aliyuncs.com/2026/06/03/f70b87c5bedd43ed896f020a5816d3b8question.png',NULL,'\\begin{aligned}&\\text{题目:}\\\\&\\text{一个两位数,十位上的数字比个位上的数字的2倍少1。如果将这个两位数的十位数字与个位数字互换,得}\\\\&\\text{到的新两位数比原两位数小27。求原来的两位数。}\\end{aligned}',NULL),('1707103528830','1780455843549','选择题','2026-06-03','数学',NULL,NULL,0,'https://ai4education.oss-cn-beijing.aliyuncs.com/2026/06/03/e40f032093ae4a3388934ac5db59ca6bquestion.png',NULL,'\\begin{aligned}&\\text{题目:}\\\\&\\text{一个两位数,十位上的数字比个位上的数字的2倍少1。如果将这个两位数的十位数字与个位数字互换,得}\\\\&\\text{到的新两位数比原两位数小27。求原来的两位数。}\\end{aligned}',NULL),('1707103528830','1780487954891','选择题','2026-06-03','数学','基本类型：读题错误','细分类型：方程设立错误',0,'https://ai4education.oss-cn-beijing.aliyuncs.com/2026/06/03/bc823e56f4524304bd35d101e76a6ed0question.png',NULL,'\\begin{aligned}&\\text{题目:}\\\\&\\text{一个两位数,十位上的数字比个位上的数字的2倍少1。如果将这个两位数的十位数字与个位数字互换,得}\\\\&\\text{到的新两位数比原两位数小27。求原来的两位数。}\\end{aligned}','\\begin{aligned}&\\text{设十位数字,个位数字 }y\\mathrm{。}\\\\&\\text{根据题意:}\\\\\\\\&&x=2y-1\\\\\\\\&\\text{新数比原数小 27,错误地列为:}\\\\\\\\&&(10y+x)-(10x+y)=27\\\\\\\\&\\text{化简得:}\\\\\\\\&&9y-9x=27\\mathrm{~\\Longrightarrow~}y-x=3\\\\\\\\&\\text{代入 }x=2y-1\\mathrm{:}\\\\\\\\&&y-(2y-1)=3\\Longrightarrow\\begin{array}{c}-y+1=3\\end{array}\\Longrightarrow\\begin{array}{c}y=-2,\\end{array}x=-5\\\\\\\\&\\text{得到负数,含去。}\\\\&\\text{错误结论:无解或题目数据有误。}\\end{aligned}'),('1707103528830','1780488387415','选择题','2026-06-03','数学','基本类型：解题错误','细分类型：方程设立错误',0,'https://ai4education.oss-cn-beijing.aliyuncs.com/2026/06/03/daf6aa77184e404e93c23ae29f029f11question.png',NULL,'\\begin{aligned}&\\text{题目:}\\\\&\\text{一个两位数,十位上的数字比个位上的数字的2倍少1。如果将这个两位数的十位数字与个位数字互换,得}\\\\&\\text{到的新两位数比原两位数小27。求原来的两位数。}\\end{aligned}','\\begin{aligned}&\\text{设十位数字,个位数字 }y\\mathrm{。}\\\\&\\text{根据题意:}\\\\\\\\&&x=2y-1\\\\\\\\&\\text{新数比原数小 27,错误地列为:}\\\\\\\\&&(10y+x)-(10x+y)=27\\\\\\\\&\\text{化简得:}\\\\\\\\&&9y-9x=27\\mathrm{~\\Longrightarrow~}y-x=3\\\\\\\\&\\text{代入 }x=2y-1\\mathrm{:}\\\\\\\\&&y-(2y-1)=3\\Longrightarrow\\begin{array}{c}-y+1=3\\end{array}\\Longrightarrow\\begin{array}{c}y=-2,\\end{array}x=-5\\\\\\\\&\\text{得到负数,含去。}\\\\&\\text{错误结论:无解或题目数据有误。}\\end{aligned}');
/*!40000 ALTER TABLE `question_basic_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_info`
--

DROP TABLE IF EXISTS `student_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_info` (
  `sid` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `avator` longtext,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `ranking` varchar(255) DEFAULT NULL,
  `grade` varchar(255) DEFAULT NULL,
  `major` varchar(255) DEFAULT NULL,
  `isLogin` tinyint DEFAULT NULL,
  PRIMARY KEY (`sid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_info`
--

LOCK TABLES `student_info` WRITE;
/*!40000 ALTER TABLE `student_info` DISABLE KEYS */;
INSERT INTO `student_info` VALUES ('1707103528830','15076709072','629848089@qq.com','男',NULL,'musi111','$2a$10$KzMezs9zuYCCWnxiO/OkueXjWSAmgXW/FrkYJuES7q7Gk0KN7i2EO','我是一名高中生','31/200','高一','数学',1);
/*!40000 ALTER TABLE `student_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-04 15:38:28
