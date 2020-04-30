package au.com.telstra.oan.dataCuration
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import com.amazon.deequ.VerificationSuite
import com.amazon.deequ.checks.{Check, CheckLevel, CheckStatus}
import com.amazon.deequ.constraints.ConstraintStatus
import org.apache.spark.sql.{DataFrame, Row}

object DataQualityWrapper {
  
  val spark = SparkSession.builder().config(new SparkConf()).enableHiveSupport().getOrCreate()

  def getDataQualityOfDF(srcDF: DataFrame): String = {
    var ret = ""
    val verificationResult = VerificationSuite()
    .onData(srcDF)
    .addCheck(
      Check(CheckLevel.Error, "unit testing my data")
        .hasSize(_ == 5) // we expect 5 rows
        .isComplete("id") // should never be NULL
        .isUnique("id") // should not contain duplicates
        .isComplete("productName") // should never be NULL
        // should only contain the values "high" and "low"
        .isContainedIn("priority", Array("high", "low"))
        .isNonNegative("numViews") // should not contain negative values
        // at least half of the descriptions should contain a url
        .containsURL("description", _ >= 0.5)
        // half of the items should have less than 10 views
        .hasApproxQuantile("numViews", 0.5, _ <= 10))
      .run()
    
    if (verificationResult.status == CheckStatus.Success) {
      ret = "The data passed the test, everything is fine!"
      ret
    } else {
      ret += "We found errors in the data:\n"
      val resultsForAllConstraints = verificationResult.checkResults
        .flatMap { case (_, checkResult) => checkResult.constraintResults }

      resultsForAllConstraints
        .filter { _.status != ConstraintStatus.Success }
        .foreach { result => ret += s"${result.constraint}: ${result.message.get}\n" }
      ret
    }
  }
}
