package org.milmsearch.core.service

import org.milmsearch.core.domain.Filter
import org.milmsearch.core.domain.MlProposal
import org.milmsearch.core.domain.MlProposalSearchResult
import org.milmsearch.core.domain.MlProposalSearchResult
import org.milmsearch.core.domain.Page
import org.milmsearch.core.domain.Sort
import org.milmsearch.core.ComponentRegistry
import org.milmsearch.core.domain.CreateMlProposalRequest
import org.milmsearch.core.domain.MlProposal
import org.milmsearch.core.domain.MlProposalFilterBy
import org.milmsearch.core.domain.MlProposalSortBy

/**
 * ML登録申請情報を管理するサービス
 */
trait MlProposalService {

  /**
   * ML登録申請情報を作成する
   * 
   * @param mlProposal ML登録申請情報
   * @return ID
   */
  def create(request: CreateMlProposalRequest): Long

  /**
   * 検索結果情報を取得する
   * 
   * @param page   取得するページ番号と1ページあたりの件数
   * @param sort   ソート方法
   * @return 検索結果情報 
   */
  def search(page: Page, sort: Sort[MlProposalSortBy.type]): MlProposalSearchResult 
  
  /**
   * 検索結果情報を取得する
   * 
   * @param filter 絞り込み条件
   * @param page   取得するページ番号と1ページあたりの件数
   * @param sort   ソート方法
   * @return 検索結果情報 
   */
  def search(filter: Filter[MlProposalFilterBy.type], page: Page, 
      sort: Sort[MlProposalSortBy.type]): MlProposalSearchResult 
  
  /**
   * ML登録申請情報を取得する
   * 
   * @param id ID
   * @return ML登録申請情報
   */
  def findById(id: Long): Option[MlProposal]

  /**
   * ML登録申請情報を更新する
   * 
   * @param id ID
   * @param mlProposal ML登録申請情報
   * @return 更新対象が存在したかどうか
   */
  def update(id: Long, mlProposal: MlProposal): Boolean


  /**
   * ML登録申請情報を削除する
   * 
   * @param id ID
   * @return 削除対象が存在したかどうか
   */
  def delete(id: Long): Boolean
}

/**
 * 検索に失敗したときの例外
 */
class SearchFailedException(msg: String) extends Exception(msg)

/**
 * MlProposalService の実装クラス
 */
class MlProposalServiceImpl extends MlProposalService {

  /**
   * ML登録申請情報 DAO
   */
  private def mpDao = ComponentRegistry.mlProposalDao()

  def create(request: CreateMlProposalRequest) = mpDao.create(request)

  def search(page: Page, sort: Sort[MlProposalSortBy.type]): MlProposalSearchResult = {
    val mlProposals = mpDao.findAll(page.toRange, sort)
    val itemsPerPage = if (mlProposals.lengthCompare(page.count.toInt) < 0) 
      mlProposals.length else page.count 
    MlProposalSearchResult(mpDao.count(), page.toRange.offset + 1, itemsPerPage, mlProposals)
  }

  def search(filter: Filter[MlProposalFilterBy.type],
      page: Page, sort: Sort[MlProposalSortBy.type]): MlProposalSearchResult = {
    if (filter.value == "") {
      throw new SearchFailedException("Filter value is empty.")
    }
    val mlProposals = mpDao.findAll(filter, page.toRange, sort)
    val itemsPerPage = if (mlProposals.lengthCompare(page.count.toInt) < 0) 
      mlProposals.length else page.count 
    MlProposalSearchResult(mpDao.count(filter), page.toRange.offset + 1, itemsPerPage, mlProposals)
  }

  def findById(id: Long) = None // TODO

  def update(id: Long, proposal: MlProposal) = false // TODO

  def delete(id: Long) = false // TODO
}
