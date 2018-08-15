/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as React from 'react';
import * as key from 'keymaster';
import { throttle } from 'lodash';
import ComponentsList from './ComponentsList';
import ListFooter from '../../../components/controls/ListFooter';
import { scrollToElement } from '../../../helpers/scrolling';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  Metric,
  Paging,
  BranchLike
} from '../../../app/types';

interface Props {
  bestValue?: string;
  branchLike?: BranchLike;
  components: ComponentMeasureEnhanced[];
  fetchMore: () => void;
  handleSelect: (component: string) => void;
  handleOpen: (component: string) => void;
  metric: Metric;
  metrics: { [metric: string]: Metric };
  paging?: Paging;
  rootComponent: ComponentMeasure;
  selectedKey?: string;
  selectedIdx?: number;
}

export default class ListView extends React.PureComponent<Props> {
  listContainer?: HTMLElement | null;

  constructor(props: Props) {
    super(props);
    this.selectNext = throttle(this.selectNext, 100);
    this.selectPrevious = throttle(this.selectPrevious, 100);
  }

  componentDidMount() {
    this.attachShortcuts();
    if (this.props.selectedKey !== undefined) {
      this.scrollToElement();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selectedKey !== undefined && prevProps.selectedKey !== this.props.selectedKey) {
      this.scrollToElement();
    }
  }

  componentWillUnmount() {
    this.detachShortcuts();
  }

  attachShortcuts() {
    key('up', 'measures-files', () => {
      this.selectPrevious();
      return false;
    });
    key('down', 'measures-files', () => {
      this.selectNext();
      return false;
    });
    key('right', 'measures-files', () => {
      this.openSelected();
      return false;
    });
  }

  detachShortcuts() {
    ['up', 'down', 'right'].forEach(action => key.unbind(action, 'measures-files'));
  }

  openSelected = () => {
    if (this.props.selectedKey !== undefined) {
      this.props.handleOpen(this.props.selectedKey);
    }
  };

  selectPrevious = () => {
    const { selectedIdx } = this.props;
    if (selectedIdx !== undefined && selectedIdx > 0) {
      this.props.handleSelect(this.props.components[selectedIdx - 1].key);
    } else {
      this.props.handleSelect(this.props.components[this.props.components.length - 1].key);
    }
  };

  selectNext = () => {
    const { selectedIdx } = this.props;
    if (selectedIdx !== undefined && selectedIdx < this.props.components.length - 1) {
      this.props.handleSelect(this.props.components[selectedIdx + 1].key);
    } else {
      this.props.handleSelect(this.props.components[0].key);
    }
  };

  scrollToElement = () => {
    if (this.listContainer) {
      const elem = this.listContainer.getElementsByClassName('selected')[0];
      if (elem) {
        scrollToElement(elem, { topOffset: 215, bottomOffset: 100 });
      }
    }
  };

  render() {
    return (
      <div ref={elem => (this.listContainer = elem)}>
        <ComponentsList
          bestValue={this.props.bestValue}
          branchLike={this.props.branchLike}
          components={this.props.components}
          metric={this.props.metric}
          metrics={this.props.metrics}
          onClick={this.props.handleOpen}
          rootComponent={this.props.rootComponent}
          selectedComponent={this.props.selectedKey}
        />
        {this.props.paging &&
          this.props.components.length > 0 && (
            <ListFooter
              count={this.props.components.length}
              loadMore={this.props.fetchMore}
              total={this.props.paging.total}
            />
          )}
      </div>
    );
  }
}