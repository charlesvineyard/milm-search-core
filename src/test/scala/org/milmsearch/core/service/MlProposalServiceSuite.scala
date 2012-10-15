package org.milmsearch.core.service
import java.net.URL

import org.milmsearch.core.dao.MlProposalDao
import org.milmsearch.core.domain.MlArchiveType
import org.milmsearch.core.domain.MlProposal
import org.milmsearch.core.domain.MlProposalStatus
import org.milmsearch.core.ComponentRegistry
import org.scalamock.scalatest.MockFactory
import org.scalamock.ProxyMockFactory
import org.scalatest.FunSuite

class MlProposalServiceSuite extends FunSuite
    with MockFactory with ProxyMockFactory {

  test("create full") {
    val mlProposal = MlProposal(
      "申請者の名前",
      "proposer@example.com",
      "MLタイトル",
      MlProposalStatus.New,
      Some(MlArchiveType.Mailman),
      Some(new URL("http://localhost/path/to/archive/")),
      Some("コメント(MLの説明など)\nほげほげ)")
    )

    val m = mock[MlProposalDao]
    m expects 'create withArgs(mlProposal) returning 1L

    expect(1L) {
      ComponentRegistry.mlProposalDao.doWith(m) {
        new MlProposalServiceImpl().create(mlProposal)
      }
    }
  }

}