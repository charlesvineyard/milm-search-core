package org.milmsearch.core.dao

import java.net.URL
import scala.collection.mutable.ListBuffer
import org.milmsearch.core.ComponentRegistry.{dateTimeService => Time}
import org.milmsearch.core.domain.CreateMlProposalRequest
import org.milmsearch.core.domain.Filter
import org.milmsearch.core.domain.MlArchiveType
import org.milmsearch.core.domain.MlProposal
import org.milmsearch.core.domain.MlProposalColumn
import org.milmsearch.core.domain.{MlProposalFilterBy => MLPFilterBy}
import org.milmsearch.core.domain.{MlProposalSortBy => MLPSortBy}
import org.milmsearch.core.domain.{MlProposalStatus => MLPStatus}
import org.milmsearch.core.domain.Range
import org.milmsearch.core.domain.Sort
import mapper.{MlProposalMapper => MLPMapper}
import mapper.{MlProposalMetaMapper => MLPMMapper}
import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Failure
import net.liftweb.common.Full
import net.liftweb.common.Loggable
import net.liftweb.mapper.By
import net.liftweb.mapper.CreatedUpdated
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.MappedDateTime
import net.liftweb.mapper.MappedEmail
import net.liftweb.mapper.MappedEnum
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedText
import net.liftweb.mapper.MaxRows
import net.liftweb.mapper.OrderBy
import net.liftweb.mapper.QueryParam
import net.liftweb.mapper.StartAt
import org.omg.CosNaming.NamingContextPackage.NotFound

/**
 * ML登録申請情報 の DAO
 */
trait MlProposalDao {

  /**
   * 検索条件と取得範囲と並び順を指定して、ML登録申請情報を検索します。
   *
   * @param filter 検索条件
   * @param range  取得範囲
   * @param sort   並び順
   * @return List[MlProposal] ML登録申請情報のリスト
   */
  def findAll(range: Range,
    sort: Option[Sort[MLPSortBy.type]] = None,
    filter: Option[Filter[MLPFilterBy.type]] = None): List[MlProposal]

  def find(id: Long): Option[MlProposal]
  def create(request: CreateMlProposalRequest): Long

  /**
   * IDを指定して、ML登録申請情報を削除します。
   *
   * @param id: ID
   * @return Boolean  ??
   */
  def delete(id: Long): Boolean

  /**
   * 検索条件を指定して、件数を数えます。
   *
   * @param filter 検索条件
   * @return Long 件数
   */
  def count(filter: Option[Filter[MLPFilterBy.type]] = None): Long

  def update(id: Long, request: CreateMlProposalRequest): Boolean{}

  def update(id: Long, column: MlProposalColumn.Value, value: String): Boolean
}

/**
 * MlProposalDao の実装クラス
 */
class MlProposalDaoImpl extends MlProposalDao with Loggable {

  def find(id: Long) = {
    MLPMMapper.find(id) match {
      case Empty => None
      case Full(mapper) => Some(toDomain(mapper))
      case Failure(message, e, _) => {
        logger.error(message, e)
        throw new DataAccessException("Failed to retrieve the data.")
      }
    }
  }

  def create(request: CreateMlProposalRequest) = toMapper(request).saveMe().id

  /**
   * ML登録申請情報ドメインを Mapper オブジェクトに変換する
   */
  private def toMapper(request: CreateMlProposalRequest): MLPMapper = {
    val now = Time().now().toDate
    MLPMMapper.create
      .proposerName(request.proposerName)
      .proposerEmail(request.proposerEmail)
      .mlTitle(request.mlTitle)
      .status(request.status.toString)
      .archiveType(request.archiveType map { _.toString } getOrElse null)
      .archiveUrl(request.archiveUrl map { _.toString } getOrElse null)
      .message(request.comment getOrElse null)
      .createdAt(now)
      .updatedAt(now)
  }

  def findAll(range: Range,
    sort: Option[Sort[MLPSortBy.type]] = None,
    filter: Option[Filter[MLPFilterBy.type]] = None) = {
    val queryParams = ListBuffer[QueryParam[MLPMapper]](
      StartAt(range.offset),
      MaxRows(range.limit))
    if (sort.isDefined) {
      queryParams += toOrderBy(sort.get)
    }
    if (filter.isDefined) {
      queryParams += toBy(filter.get)
    }

    MLPMMapper.findAll(queryParams: _*) map toDomain
  }

  def count(filter: Option[Filter[MLPFilterBy.type]] = None) =
    if (filter.isDefined)
      MLPMMapper.count(toBy(filter.get))
    else
      MLPMMapper.count()

  private def toDomain(mapper: MLPMapper) =
    MlProposal(
      mapper.id.get,
      mapper.proposerName.get,
      mapper.proposerEmail.get,
      mapper.mlTitle.get,
      MLPStatus.withName(mapper.status.get),
      Option(MlArchiveType.withName(mapper.archiveType.get)),
      Option(new URL(mapper.archiveUrl.get)),
      Option(mapper.message.get),
      mapper.createdAt.get,
      mapper.updatedAt.get)

  /**
   * 指定された情報のmapperを返す
   * @param id ID
   * @return Option[MLPMapper]
   * @throws FindException
   */
  private def findMapper(id: Long):Option[MLPMapper] =
    MLPMMapper.find(id) match {
      case Full(mapper) => Some(mapper)
      case Empty => None
      case Failure(message, e, _) => throw new FindException(message)
    }

  def toBy(filter: Filter[MLPFilterBy.type]) = filter match {
    case Filter(MLPFilterBy.Status, v: MLPStatus.Value) =>
      By(MLPMMapper.status, v.toString)
    case _ => throw new NoSuchFieldException(
      "Can't convert Filter to By")
  }

  def toOrderBy(sort: Sort[MLPSortBy.type]) = {
    import MLPSortBy._
    import MLPMMapper._
    OrderBy(sort.column match {
      case MlTitle => mlTitle
      case Status => status
      case ArchiveType => archiveType
      case CreatedAt => createdAt
      case UpdatedAt => updatedAt
      case _ => throw new NoSuchFieldException(
        "Can't convert Filter to By")
    }, DaoHelper.toAscOrDesc(sort.sortOrder))
  }

  def delete(id: Long): Boolean = {
    val mapper = findMapper(id)
    mapper match {
      case Some(m) => MLPMMapper.delete_!(m)
      case None => false
    }
  }

  def update(id: Long, request: CreateMlProposalRequest) = {
    val target: Box[mapper.MlProposalMapper] = mapper.MlProposalMetaMapper.find(id)
    target match {
      case Full(row) => mapper.MlProposalMetaMapper.save(row)
      case Empty => false
      case Failure(message, e, _) => throw e openOr new RuntimeException(message)
    }
  }

  def update(id: Long, column: MlProposalColumn.Value, value: String) = {
    findMapper(id) match {
      case None => false
      case Some(mlpMapper) => {
        import MlProposalColumn._
        column match {
          case ProposerName  => mlpMapper.proposerName.set(value)
          case ProposerEmail => mlpMapper.proposerEmail.set(value)
          case MlTitle       => mlpMapper.mlTitle.set(value)
          case Status        => mlpMapper.status.set(value)
          case ArchiveType   => mlpMapper.archiveType.set(value)
          case ArchiveUrl    => mlpMapper.archiveUrl.set(value)
          case Comment       => mlpMapper.message.set(value)
          // XXX CreatedAt と UpdateAt は Date型でないと set できなかったので保留
          case notMlpColumn => throw new NoSuchFieldException(
           "Can't update [%s]'s column." formatted notMlpColumn.toString)
        }
        mlpMapper.save()
      }
    }
  }
}

/**
 * O/R マッパー
 */
package mapper {

  /**
   * ML登録申請情報テーブルの操作を行う
   */
  private[dao] object MlProposalMetaMapper
    extends MlProposalMapper
    with LongKeyedMetaMapper[MlProposalMapper] {
    override def dbTableName = "ml_proposal"
    override def fieldOrder = List(
      id, proposerName, proposerEmail, mlTitle, status,
      archiveType, archiveUrl, message, createdAt, updatedAt)
  }

  /**
   * ML登録申請情報のモデルクラス
   */
  private[dao] class MlProposalMapper extends
      LongKeyedMapper[MlProposalMapper] with IdPK {
    def getSingleton = MlProposalMetaMapper

    object proposerName extends MappedString(this, 200)
    object proposerEmail extends MappedEmail(this, 200)
    object mlTitle extends MappedString(this, 200)
    object status extends MappedString(this, 200)
    object archiveType extends MappedString(this, 200)
    object archiveUrl extends MappedText(this)
    object message extends MappedText(this)
    object createdAt extends MappedDateTime(this)
    object updatedAt extends MappedDateTime(this)
  }
}
