# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='tableentry',
            name='link_drop_str',
            field=models.CharField(max_length=10, null=True, blank=True),
        ),
    ]
