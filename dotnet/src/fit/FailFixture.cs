// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
namespace fit
{
	public class FailFixture : Fixture
	{
		public override void DoTable(Parse p)
		{
			Wrong(p);
		}
	}
}