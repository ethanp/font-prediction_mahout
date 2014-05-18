import java.io.{PrintWriter, File}
import org.apache.mahout.cf.taste.eval.{IRStatistics, RecommenderIRStatsEvaluator, RecommenderBuilder, RecommenderEvaluator}
import org.apache.mahout.cf.taste.impl.eval.{RMSRecommenderEvaluator, GenericRecommenderIRStatsEvaluator, AverageAbsoluteDifferenceRecommenderEvaluator}
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood
import org.apache.mahout.cf.taste.impl.recommender.{GenericItemBasedRecommender, GenericUserBasedRecommender}
import org.apache.mahout.cf.taste.impl.similarity.{EuclideanDistanceSimilarity, PearsonCorrelationSimilarity}
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood
import org.apache.mahout.cf.taste.recommender.{Recommender, RecommendedItem}
import org.apache.mahout.cf.taste.similarity.{ItemSimilarity, UserSimilarity}
import org.apache.mahout.common.RandomUtils
import org.apache.mahout.cf.taste.common.Weighting


/**
 * Based on: the one example that seems to be used by
 * every Mahout tutorial out there
 */

/**
 * Evaluation:
 *
 * "OutFileData.csv" // everything
 * average absolute difference 12.5
 *
 * "10AndUpData.csv" // only pairs with at least 10 instances in the dataset
 * average absolute difference 13.8
 */

object UseMahout extends App {

  // ensures consistency between different evaluation runs
  RandomUtils.useTestSeed()



  val model: DataModel = new FileDataModel(new File("OutFileData.csv"))

  val evaluator: RecommenderEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator
  val irEvaluator: RecommenderIRStatsEvaluator = new GenericRecommenderIRStatsEvaluator


  val rmse: RecommenderEvaluator = new RMSRecommenderEvaluator

  val recommenderBuilder: RecommenderBuilder = new RecommenderBuilder() {
    @Override
    def buildRecommender(model: DataModel): Recommender = {
      val similarity: UserSimilarity = new PearsonCorrelationSimilarity(model)
      val itemSimilarity: ItemSimilarity = new PearsonCorrelationSimilarity(model)

      // another option
      val euclideanSimilarity: UserSimilarity = new EuclideanDistanceSimilarity(model)

      // the first parameter is the number of neighbors
      val neighborhood: UserNeighborhood = new NearestNUserNeighborhood(2, similarity, model)

      // another option
      val itemRecommender: Recommender = new GenericItemBasedRecommender(model, itemSimilarity)

      val userRecommender: Recommender = new GenericUserBasedRecommender(model, neighborhood, similarity)
      userRecommender
    }
  }

  // 70% training, evaluation on the whole dataset
  val score: Double = evaluator.evaluate(recommenderBuilder, null, model, 0.7, 1.0)
  println(s"Score: $score")

  val rmseScore: Double = rmse.evaluate(recommenderBuilder, null, model, 0.7, 1.0)
  println(s"RMSE Score: $rmseScore")

  // Note: this is more for a "boolean" data model situation, where there are no notions of preference value
  val irStats: IRStatistics = irEvaluator.evaluate(
    recommenderBuilder, null, model, null, 5,
    GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, 1.0
  )

  // precision @ 5, Recall @ 5, F1 (combination)
  println(
    s"""Precision: ${irStats.getPrecision},
       |Recall: ${irStats.getRecall},
       |F1: ${irStats.getF1Measure}""".stripMargin
  )
}
