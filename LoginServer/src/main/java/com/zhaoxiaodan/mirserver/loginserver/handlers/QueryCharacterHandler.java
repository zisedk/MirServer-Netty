package com.zhaoxiaodan.mirserver.loginserver.handlers;

import com.zhaoxiaodan.mirserver.db.DB;
import com.zhaoxiaodan.mirserver.gameserver.entities.User;
import com.zhaoxiaodan.mirserver.loginserver.LoginClientPackets;
import com.zhaoxiaodan.mirserver.loginserver.LoginServerPackets;
import com.zhaoxiaodan.mirserver.network.Handler;
import com.zhaoxiaodan.mirserver.network.Protocol;
import com.zhaoxiaodan.mirserver.network.packets.ClientPacket;
import com.zhaoxiaodan.mirserver.network.packets.ServerPacket;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * +-------------------------------------------------+
 * |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
 * +--------+-------------------------------------------------+----------------+
 * |00000000| 01 00 00 00 08 02 00 00 01 00 00 00 2a c5 d6 c1 |............*...|
 * |00000010| ba b9 fe b9 fe 2f 30 2f 32 2f 32 2f 30 2f 00 00 |...../0/2/2/0/..|
 * |00000020| 00 00 00 00 00 00 00 00                         |........        |
 * +--------+-------------------------------------------------+----------------+
 * +-------------------------------------------------+
 * |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
 * +--------+-------------------------------------------------+----------------+
 * |00000000| 02 00 00 00 08 02 00 00 01 00 00 00 2a c5 d6 c1 |............*...|
 * |00000010| ba b9 fe b9 fe 2f 30 2f 32 2f 32 2f 30 2f 70 61 |...../0/2/2/0/pa|
 * |00000020| 6e 67 6c 69 61 6e 67 2f 32 2f 35 2f 30 2f 31 2f |ngliang/2/5/0/1/|
 * |00000030| 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 |................|
 * +--------+-------------------------------------------------+----------------+
 */
public class QueryCharacterHandler extends Handler {

	@Override
	public void onPacket(ClientPacket packet) throws Exception {
		LoginClientPackets.QueryCharacter request = (LoginClientPackets.QueryCharacter) packet;
		User                        user;
		if ((user = (User)session.get("user")) == null) {
			List<User> list = DB.query(User.class, Restrictions.eq("loginId", request.loginId));
			if (list.size() == 0) {
				session.sendPacket(new ServerPacket(Protocol.SM_CERTIFICATION_FAIL));
				return;
			}
			user = list.get(0);
		}

		if (user.certification == request.cert) {
			session.put("user", user);
			session.sendPacket(new LoginServerPackets.QueryCharactorOk(user.players));
		} else {
			session.sendPacket(new ServerPacket(Protocol.SM_CERTIFICATION_FAIL));
			return;
		}
	}

}